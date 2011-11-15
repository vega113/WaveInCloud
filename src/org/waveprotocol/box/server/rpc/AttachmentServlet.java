/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.server.rpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An attachment servlet is a simple servlet that serves up attachments from a
 * provided store.
 */
@SuppressWarnings("serial")
@Singleton
public class AttachmentServlet extends HttpServlet {
  private static final Log LOG = Log.get(AttachmentServlet.class);

  private final AttachmentStore store;

  @Inject
  private AttachmentServlet(AttachmentStore store) {
    this.store = store;
  }

  /**
   * Get the attachment id from the URL in the request.
   * 
   * @param request
   * @return the id of the referenced attachment.
   */
  private static String getAttachmentIdFromRequest(HttpServletRequest request) {
    if (request.getPathInfo().length() == 0) {
      return "";
    }

    // Discard the leading '/' in the pathinfo. Whats left will be the
    // attachment id.
    return request.getPathInfo().substring(1);
  }

  /**
   * Get the attachment id from the URL in the request.
   * 
   * @param request
   * @return the id of the referenced attachment.
   */
  private static String getFileNameFromRequest(HttpServletRequest request) {
    String fileName = request.getParameter("fileName");
    return fileName != null ? fileName : "";
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: Authenticate that the user has permission to access the attachment.

    String attachmentId = getAttachmentIdFromRequest(request);
    String fileName = getFileNameFromRequest(request);

    if (attachmentId.isEmpty() || fileName.isEmpty()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    AttachmentData data = store.getAttachment(attachmentId);

    if (data == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    String mimetype = getMimeTypeByFileName(fileName);
    response.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
    response.setContentLength((int) data.getContentSize());
    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentLength((int) data.getContentSize());
    response.setDateHeader("Last-Modified", data.getLastModifiedDate().getTime());
    data.writeDataTo(response.getOutputStream());

    LOG.info("Fetched attachment with id '" + attachmentId + "'");
  }

  private String getMimeTypeByFileName(String fileName) {
    String mimeType = "application/octet-stream";
    if (fileName.endsWith(".ico")) {
      mimeType = "image/png;";
    } else if (fileName.endsWith(".xml")) {
      mimeType = "text/plain;";
    } else if (fileName.endsWith(".png")) {
      mimeType = "image/png;";
    } else if (fileName.endsWith(".html")) {
      mimeType = "text/html;";
    } else if (fileName.endsWith(".doc")) {
      mimeType = "application/msword;";
    } else if (fileName.endsWith(".pdf")) {
      mimeType = "application/pdf;";
    } else if (fileName.endsWith(".mp3")) {
      mimeType = "audio/mpeg;";
    } else if (fileName.endsWith(".gif")) {
      mimeType = "image/gif;";
    }
    return mimeType;
  }

  @Override
  protected void doPut(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    // TODO: Authenticate the attachment data

    String attachmentId = getAttachmentIdFromRequest(request);
    if (attachmentId.length() == 0) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    store.storeAttachment(attachmentId, request.getInputStream());

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write("<html><body><h1>Data written</h1></body></html>");

    LOG.info("Added attachment with id '" + attachmentId + "'");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {

    // process only multipart requests
    if (ServletFileUpload.isMultipartContent(req)) {

      // Create a factory for disk-based file items
      FileItemFactory factory = new DiskFileItemFactory();

      // Create a new file upload handler
      ServletFileUpload upload = new ServletFileUpload(factory);

      // Parse the request
      try {
        @SuppressWarnings("unchecked")
        List<FileItem> items = upload.parseRequest(req);
        String id = null;
        FileItem fileItem = null;
        for (FileItem item : items) {
          // process only file upload - discard other form item types
          if (item.isFormField()) {
            if (item.getFieldName().equals("attachmentId")) {
              id = item.getString();
            }
          } else {
            fileItem = item;
          }
        }
        String fileName = fileItem.getName();
        // Get only the file name not whole path.
        if (fileName != null) {
          fileName = FilenameUtils.getName(fileName);
          if (store.storeAttachment(id, fileItem.getInputStream())) {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            String msg =
                String.format("The file with name: %s and id: %s was created successfully.",
                    fileName, id);
            LOG.fine(msg);
            resp.getWriter().print("OK");
          } else {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            String msg = "Attachment ID " + id + " already exists!";
            LOG.warning(msg);
            resp.getWriter().print(msg);
          }
          resp.flushBuffer();
        }
      } catch (Exception e) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "An error occurred while creating the file : " + e.getMessage());
      }

    } else {
      resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
          "Request contents type is not supported by the servlet.");
    }
  }
}
