/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.run;

import automenta.netention.Core;
import automenta.netention.web.SpacetimeWebServer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import spangraph.InfiniPeer;


/**
 *
 * @author me
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("spacetime")
                .description("Decentralized Environment Awareness System");
        //parser.addMutuallyExclusiveGroup("")
        
        
        ArgumentGroup peerGroup = parser.addArgumentGroup("P2P");
        
        
        peerGroup.addArgument("-p2pport").type(Integer.class).required(false).
                help("Port to connect P2P network (ensure it is not firewalled)");
        peerGroup.addArgument("-p2pseeds").type(String.class).setDefault("").required(false).
                help("Comma-separated (no spaces) list of host:port pairs");
        
        ArgumentGroup webGroup = parser.addArgumentGroup("Web server");
        webGroup.addArgument("-webport").type(Integer.class).required(false).setDefault(8080).help("Port to connect Web Server");
        
        ArgumentGroup netGroup = parser.addArgumentGroup("Network");
        netGroup.addArgument("-localhost").type(String.class).required(false).setDefault("localhost").help("Host to bind servers, if different from localhost");

        MutuallyExclusiveGroup dbGroup = parser.addMutuallyExclusiveGroup("Database");
        //dbGroup.addArgument("-esserver").type(String.class).help("ElasticSearch server host:port");
        
        //dbGroup.addArgument("-espath").type(String.class).help("Elasticsearch embedded DB path");

        MutuallyExclusiveGroup dbOptGroup = parser.addMutuallyExclusiveGroup("Database Options");
        //dbOptGroup.addArgument("-esindex").type(String.class).help("ElasticSearch index name").required(false).setDefault("spacetime");
        

        try {
            Namespace res = parser.parseArgs(args);
            //System.out.println(res);            
            
            
            String esPath = res.getString("espath");
            String esServer = res.getString("esserver");
            String esIndex = res.getString("esindex");
            Integer webPort = res.getInt("webport");
            String webHost = res.getString("webhost");
            Integer p2pPort = res.getInt("p2pport");
            String localhost = res.getString("localhost");
            
            //System.out.println(esPath + " " + esServer + " " + esIndex);
            
            SpacetimeWebServer w = null;

            InfiniPeer peer = InfiniPeer.local("anon_" + Core.uuid());

            if (webPort!=null) {
                w = new SpacetimeWebServer( peer, localhost, webPort);
            }
//
//            if (e!=null && p2pPort!=null) {
//                p = new SpacetimePeer(localhost, p2pPort);
//                p.peer.add(e);
//
//                //TODO add seeds
//            }
//
            if (w!=null)
                w.start();
            
            /*System.out.println(((Accumulate) res.get("accumulate"))
                    .accumulate((List<Integer>) res.get("integers")));*/
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }        
    }
}
