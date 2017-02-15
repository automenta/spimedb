/*
 * Copyright 1999,2004 The Apache Software Foundation.
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
 */
package spimedb.server.webdav.methods;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spimedb.server.webdav.ITransaction;
import spimedb.server.webdav.IWebdavStore;
import spimedb.server.webdav.StoredObject;
import spimedb.server.webdav.WebdavStatus;
import spimedb.server.webdav.exceptions.AccessDeniedException;
import spimedb.server.webdav.exceptions.LockFailedException;
import spimedb.server.webdav.exceptions.ObjectAlreadyExistsException;
import spimedb.server.webdav.exceptions.ObjectNotFoundException;
import spimedb.server.webdav.exceptions.WebdavException;
import spimedb.server.webdav.locking.ResourceLocks;

public class DoDelete extends AbstractMethod {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoDelete.class);

    private final IWebdavStore _store;
    private final ResourceLocks _resourceLocks;
    private final boolean _readOnly;

    public DoDelete(IWebdavStore store, ResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void run(ITransaction transaction, HttpServletRequest req,
                    HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- {}", this.getClass().getName());

        if (!_readOnly) {
            String path = getRelativePath(req);
            String parentPath = getParentPath(getCleanPath(path));

            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
                errorList.put(parentPath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return; // parent is locked
            }

            if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
                errorList.put(path, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return; // resource is locked
            }

            String tempLockOwner = "doDelete" + System.currentTimeMillis()
                    + req.toString();
            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {
                    errorList = new Hashtable<>();
                    deleteResource(transaction, path, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction,
                            path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }

    }

    /**
     * deletes the recources at "path"
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     * @throws IOException
     *      when an error occurs while sending the response
     */
    public void deleteResource(ITransaction transaction, String path,
            Hashtable<String, Integer> errorList, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, WebdavException {

        resp.setStatus(WebdavStatus.SC_NO_CONTENT);

        if (!_readOnly) {

            StoredObject so = _store.get(transaction, path);
            if (so != null) {

                if (so.isResource()) {
                    _store.remove(transaction, path);
                } else {
                    if (so.isFolder()) {
                        deleteFolder(transaction, path, errorList, req, resp);
                        _store.remove(transaction, path);
                    } else {
                        resp.sendError(WebdavStatus.SC_NOT_FOUND);
                    }
                }
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
            so = null;

        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
     * 
     * helper method of deleteResource() deletes the folder and all of its
     * contents
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param path
     *      the folder to be deleted
     * @param errorList
     *      all errors that ocurred
     * @param req
     *      HttpServletRequest
     * @param resp
     *      HttpServletResponse
     * @throws WebdavException
     *      if an error in the underlying store occurs
     */
    private void deleteFolder(ITransaction transaction, String path,
            Hashtable<String, Integer> errorList, HttpServletRequest req,
            HttpServletResponse resp) throws WebdavException {

        String[] children = _store.in(transaction, path);
        children = children == null ? new String[] {} : children;
        StoredObject so = null;
        for (int i = children.length - 1; i >= 0; i--) {
            children[i] = '/' + children[i];
            try {
                so = _store.get(transaction, path + children[i]);
                if (so.isResource()) {
                    _store.remove(transaction, path + children[i]);

                } else {
                    deleteFolder(transaction, path + children[i], errorList,
                            req, resp);

                    _store.remove(transaction, path + children[i]);

                }
            } catch (AccessDeniedException e) {
                errorList.put(path + children[i], WebdavStatus.SC_FORBIDDEN);
            } catch (ObjectNotFoundException e) {
                errorList.put(path + children[i], WebdavStatus.SC_NOT_FOUND);
            } catch (WebdavException e) {
                errorList.put(path + children[i], WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        so = null;

    }

}
