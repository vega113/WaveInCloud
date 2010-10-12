/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.waveprotocol.box.client.webclient.client;

import org.waveprotocol.box.client.webclient.util.Log;
import org.waveprotocol.box.client.webclient.waveclient.common.WaveViewServiceImpl;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelFactory;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelImpl;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexerImpl.LoggerContext;
import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveView;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveViewImpl;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDocument;
import org.waveprotocol.wave.concurrencycontrol.wave.DuplexOpSinkFactory;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.util.MutableDocumentProxy;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.schema.impl.ConversationSchemas;
import org.waveprotocol.wave.model.util.ImmediateExcecutionScheduler;
import org.waveprotocol.wave.model.util.Scheduler;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl.Factory;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;

import java.util.Random;

class CcStackManager {
  public static class SimpleCcDocument implements CcDocument {
    class MutableDocProxy extends MutableDocumentProxy<Doc.N, Doc.E, Doc.T> implements Document {
      MutableDocProxy(MutableDocument<Doc.N, Doc.E, Doc.T> doc) {
        super();
        setDelegate(doc);
      }
    }

    private final BlipView blipView;

    public SimpleCcDocument(BlipView blipView) {
      this.blipView = blipView;
    }

    @Override
    public boolean flush(final Runnable resume) {
      return blipView.getEditor().flushAsync(new com.google.gwt.user.client.Command() {
        @Override
        public void execute() {
          resume.run();
        }
      });
    }

    public BlipView getBlipView() {
      return blipView;
    }

    @Override
    public Document getMutableDocument() {
      @SuppressWarnings("unchecked")
      MutableDocProxy docProxy =
          new MutableDocProxy((MutableDocument) ((EditorImpl) blipView.getEditor()).mutable());
      return docProxy;
    }

    @Override
    public void init(SilentOperationSink<? super BufferedDocOp> outputSink) {
      // nothing to do.
    }

    @Override
    public DocInitialization asOperation() {
      return blipView.getEditor().getDocumentInitialization();
    }

    @Override
    public void consume(DocOp op) {
      blipView.getEditor().getContent().consume(op);
    }
  }

  private static final Log LOG = Log.get(CcStackManager.class);

  final WaveViewServiceImpl viewService;
  final OperationChannelMultiplexer mux;
  final CcBasedWaveView view;
  final LoggerBundle loggerBundle;
  final CcBasedWaveViewImpl.CcDocumentFactory<CcDocument> docFactory;

  public CcStackManager(final WaveViewServiceImpl viewService,
      final SimpleCcDocumentFactory docFactory, ParticipantId participant) {
    this.viewService = viewService;
    loggerBundle = new AbstractLogger(new LogSink() {

      @Override
      public void lazyLog(Level level, Object... messages) {
        StringBuilder out = new StringBuilder();
        for (Object msg : messages) {
          out.append(msg);
        }
        log(level, out.toString());
      }

      @Override
      public void log(Level level, String message) {
        switch (level) {
          case FATAL:
          case ERROR:
            LOG.severe("LB: " + message);
            break;
          case TRACE:
            LOG.info("LB: " + message);
            break;
        }
      }
    }) {

      @Override
      protected boolean shouldLog(Level level) {
        // TODO Auto-generated method stub
        return true;
      }

      @Override
      public boolean isModuleEnabled() {
        // TODO Auto-generated method stub
        return true;
      }

    };

    ViewChannelFactory viewFactory = ViewChannelImpl.factory(viewService, loggerBundle);
    this.docFactory = docFactory;
    Scheduler scheduler = new ImmediateExcecutionScheduler();

    Factory dataFactory = WaveletDataImpl.Factory.create(docFactory);

    mux = new OperationChannelMultiplexerImpl(viewService.getWaveId(),
        viewFactory,
        dataFactory,
        new LoggerContext(loggerBundle, loggerBundle, loggerBundle, loggerBundle),
        null,
        scheduler);

    IdGenerator idGen = new IdGeneratorImpl(participant.getDomain(), new Seed() {
      Random r = new Random();

      @Override
      public String get() {
        return Long.toString(Math.abs(r.nextLong()), 36);
      }
    });

    view = CcBasedWaveViewImpl.create(docFactory,
        new ConversationSchemas(),
        viewService.getWaveId(),
        participant,
        mux,
        IdFilters.ALL_IDS,
        idGen,
        loggerBundle,
        new BasicWaveletOperationContextFactory(participant),
        ParticipationHelper.IGNORANT,
        new CcBasedWaveViewImpl.DisconnectedHandler() {
          @Override
          public void onWaveDisconnected(CorruptionDetail detail) {
            // do nothing -- maybe notify the user?
          }
        },
        WaveletConfigurator.ADD_CREATOR,
        DuplexOpSinkFactory.PASS_THROUGH);
    docFactory.setView(view);
  }

  public WaveId getWaveId() {
    return viewService.getWaveId();
  }
}
