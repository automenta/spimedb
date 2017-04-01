package spimedb.server;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by me on 4/1/17.
 */
public class ResourceManagerChain implements ResourceManager {

    final List<ResourceManager> managers = new CopyOnWriteArrayList<>();


    public ResourceManagerChain() {

    }

    public void add(ResourceManager a) {
        managers.add(a);
    }

    @Override
    public Resource getResource(String p) {
        for (int i = 0, managersSize = managers.size(); i < managersSize; i++) {
            ResourceManager x = managers.get(i);
            try {
                Resource y = x.getResource(p);
                if (y != null)
                    return y;
            } catch (IOException ignored) {
                //ignore
            }
        }
        return null;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return false;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {

    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {

    }

    @Override
    public void close() throws IOException {
        managers.forEach(m -> {
            try {
                m.close();
            } catch (IOException e) {
            }
        });
    }
}
