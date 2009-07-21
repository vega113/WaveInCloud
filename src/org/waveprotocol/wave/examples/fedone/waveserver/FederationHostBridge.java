// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Used to mark classes to be injected between the Wave Server and the
 * Federation Host.
 * 
 *
 */
@BindingAnnotation @Target({ FIELD, PARAMETER, METHOD }) @Retention(RUNTIME)
public @interface FederationHostBridge {}
