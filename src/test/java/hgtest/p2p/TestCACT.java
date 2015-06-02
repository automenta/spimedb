package hgtest.p2p;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import hgtest.bean.ComplexBean;
import hgtest.bean.PlainBean;
import hgtest.bean.SimpleBean;
import junit.framework.Assert;
import mjson.Json;
import org.hypergraphdb.*;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.atom.HGSubsumes;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.PeerConfig;
import org.hypergraphdb.peer.PeerHyperNode;
import org.hypergraphdb.peer.bootstrap.AffirmIdentityBootstrap;
import org.hypergraphdb.peer.bootstrap.CACTBootstrap;
import org.hypergraphdb.peer.cact.DefineAtom;
import org.hypergraphdb.peer.cact.GetAtom;
import org.hypergraphdb.peer.cact.RemoteQueryExecution;
import org.hypergraphdb.peer.cact.RunRemoteQuery;
import org.hypergraphdb.peer.workflow.WorkflowState;
import org.hypergraphdb.query.HGQueryCondition;
import org.hypergraphdb.util.HGUtils;
import org.hypergraphdb.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCACT
{
	boolean both = true;
	private boolean which = false; // true for peer 1, falsefor peer 2
	private HyperGraph graph1, graph2;
	private HyperGraphPeer peer1, peer2;
	private File locationBase = new File(Files.createTempDir().getPath());
	private File locationGraph1 = new File(locationBase, "hgp2p1");
	private File locationGraph2 = new File(locationBase, "hgp2p2");

	private HyperGraphPeer startPeer(String dblocation, String username, String hostname)
	{

		Json config = Json.object();
		config.set(PeerConfig.INTERFACE_TYPE, "org.hypergraphdb.peer.xmpp.XMPPPeerInterface");
		config.set(PeerConfig.LOCAL_DB, dblocation);
		Json interfaceConfig = Json.object();
		interfaceConfig.set("user", username);
		interfaceConfig.set("password", "hgpassword");
		interfaceConfig.set("serverUrl", hostname);
		interfaceConfig.set("room", "hgtest@conference.chat." + hostname);
		interfaceConfig.set("autoRegister", true);
		config.set(PeerConfig.INTERFACE_CONFIG, interfaceConfig);

		// bootstrap activities
		config.set(
				PeerConfig.BOOTSTRAP,
				Json.array(Json.object("class", AffirmIdentityBootstrap.class.getName(), "config", Json.object()),
						Json.object("class", CACTBootstrap.class.getName(), "config", Json.object())));

		HyperGraphPeer peer = new HyperGraphPeer(config);
		Future<Boolean> startupResult = peer.start();
		try
		{
			if (startupResult.get())
			{
				System.out.println("Peer " + username + " started successfully.");
			}
			else
			{
				System.out.println("Peer failed to start.");
				HGUtils.throwRuntimeException(peer.getStartupFailedException());
			}
		}
		catch (Exception e)
		{
			peer.stop();
			throw new RuntimeException(e);
		}
		return peer;
	}

	@Before
	public void setUp()
	{
		String localhost = "localhost";
		try
		{
			localhost = InetAddress.getLocalHost().getHostName();
		}
		catch (Throwable t)
		{
		}
		if (which || both)
		{
			HGUtils.dropHyperGraphInstance(locationGraph1.getAbsolutePath());
			graph1 = HGEnvironment.get(locationGraph1.getAbsolutePath());
			peer1 = startPeer(locationGraph1.getAbsolutePath(), "cact1", localhost);
		}
		if (!which || both)
		{
			HGUtils.dropHyperGraphInstance(locationGraph2.getAbsolutePath());
			graph2 = HGEnvironment.get(locationGraph2.getAbsolutePath());
			peer2 = startPeer(locationGraph2.getAbsolutePath(), "cact2", localhost);
		}
	}

	@After
	public void tearDown()
	{
		if (which || both)
		{
			try
			{
				peer1.stop();
			}
			catch (Throwable t)
			{
			}
			try
			{
				graph1.close();
			}
			catch (Throwable t)
			{
			}
			try
			{
				HGUtils.dropHyperGraphInstance(locationGraph1.getAbsolutePath());
			}
			catch (Throwable t)
			{
			}
		}
		if (!which || both)
		{
			try
			{
				peer2.stop();
			}
			catch (Throwable t)
			{
			}
			try
			{
				graph2.close();
			}
			catch (Throwable t)
			{
			}
			try
			{
				HGUtils.dropHyperGraphInstance(locationGraph1.getAbsolutePath());
			}
			catch (Throwable t)
			{
			}
		}
	}

	@Test
	public void testDefineAtom()
	{
		HGHandle fromPeer1 = graph1.add("From Peer1");
		peer1.getActivityManager().initiateActivity(new DefineAtom(peer1, fromPeer1, peer2.getIdentity()));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String received = graph2.get(graph1.getPersistentHandle(fromPeer1));
		if (received != null)
			System.out.println("Peer 2 received " + received);
		else
			assertTrue("peer 2 didn't received message", false);
	}

	@Test
	public void testGetAtom()
	{
		Object atom = "Get Atom In Peer1";
		HGHandle fromPeer1 = hg.assertAtom(graph1, atom);
		try
		{
			while (peer2.getConnectedPeers().isEmpty())
				Thread.sleep(1);
			GetAtom activity = new GetAtom(peer2, graph1.getPersistentHandle(fromPeer1), peer1.getIdentity());
			peer2.getActivityManager().initiateActivity(activity);
			activity.getFuture().get();
			assertEquals(activity.getState(), WorkflowState.Completed);
			assertEquals(activity.getOneAtom(), atom);
		}
		catch (Throwable t)
		{
			assertTrue("Exception during GetAtom activity: " + t.toString(), false);
		}
	}

	@Test
	public void testRemoveAtom()
	{
		Object atom = "Get Atom In Peer1";
		HGHandle fromPeer1 = hg.assertAtom(graph1, atom);
		try
		{
			while (peer2.getConnectedPeers().isEmpty())
				Thread.sleep(500);
			GetAtom activity = new GetAtom(peer2, graph1.getPersistentHandle(fromPeer1), peer1.getIdentity());
			peer2.getActivityManager().initiateActivity(activity);
			activity.getFuture().get();
			assertEquals(activity.getState(), WorkflowState.Completed);
			assertEquals(activity.getOneAtom(), atom);
		}
		catch (Throwable t)
		{
			assertTrue("Exception during GetAtom activity: " + t, false);
		}
	}

	@Test
	public void testRemoteQuery()
	{
		try
		{
			while (peer2.getConnectedPeers().isEmpty())
				Thread.sleep(500);

			HGQueryCondition expression = hg.dfs(graph1.getTypeSystem().getTop(), hg.type(HGSubsumes.class), null, false, true);
			RemoteQueryExecution<HGHandle> activity = new RemoteQueryExecution<HGHandle>(peer1, expression, peer2.getIdentity());
			peer1.getActivityManager().initiateActivity(activity);
			activity.getState().getFuture(RemoteQueryExecution.ResultSetOpen).get();
			List<HGHandle> received = new ArrayList<HGHandle>();
			HGSearchResult<HGHandle> rs = activity.getSearchResult();
			while (rs.hasNext())
				received.add(rs.next());
			rs.close();
			assertEquals(hg.findAll(graph2, expression), received);
		}
		catch (Throwable t)
		{
			assertTrue("Exception during RemoteQuery Activity activity " + t, false);
		}
	}

	@Test
	public void testRemoteQueryBulk()
	{
		try
		{
			HGQueryCondition expression = hg.dfs(graph1.getTypeSystem().getTop(),
												hg.type(HGSubsumes.class), null, false, true);
			RunRemoteQuery activity = new RunRemoteQuery(peer1, expression, false, -1, peer2.getIdentity());
			peer1.getActivityManager().initiateActivity(activity);
			activity.getFuture().get();
			@SuppressWarnings("unchecked")
			List<HGHandle> received = (List<HGHandle>) (List<?>) activity.getResult();
			List<HGHandle> expected = hg.findAll(graph2, expression);
			assertEquals(received, expected);

			List<Object> beans = new ArrayList<Object>();
			for (int i = 0; i < 158; i++)
				beans.add(new PlainBean(i));
			for (Object x : beans)
				graph2.add(x);

			activity = new RunRemoteQuery(peer1, hg.type(PlainBean.class), true, -1, peer2.getIdentity());
			peer1.getActivityManager().initiateActivity(activity);
			activity.getFuture().get();

			List<Object> result = new ArrayList<Object>();
			for (Object x : activity.getResult())
			{
				// System.out.println(((Pair<?, Object>)x).getSecond());
				result.add(((Pair<?, Object>) x).getSecond());
			}
			//assertEquals(Sets.newHashSet(result.toArray()), Sets.newHashSet(beans.toArray()));
            equalArrays((result.toArray()), (beans.toArray()));

        }
		catch (Throwable t)
		{
			assertTrue("Exception during RemoteQuery Activity activity " + t, false);
		}
	}

    private void equalArrays(Object[] a, Object[] b) {
        assertEquals(Sets.newHashSet(a), Sets.newHashSet(b));
    }

    @Test
	public void testHyperNode() throws Throwable
	{
		while (peer2.getConnectedPeers().isEmpty())
			Thread.sleep(500);
		HyperNode node = new PeerHyperNode(peer1, peer2.getIdentity());

		HGHandle stringType = graph1.getTypeSystem().getTypeHandle(String.class);
		HGHandle beanType = graph1.getTypeSystem().getTypeHandle(ComplexBean.class);
		HGHandle intType = graph1.getTypeSystem().getTypeHandle(Integer.class);
		HGHandle listType = graph1.getTypeSystem().getTypeHandle(ArrayList.class);

		HGHandle x1 = node.add("Hello World", stringType, 0);
		HGHandle x2 = graph1.getHandleFactory().makeHandle();
		ComplexBean complexBean = new ComplexBean();
		complexBean.setStableField("HYPERNODE");
		complexBean.setStableNested(new SimpleBean());
		node.define(x2, beanType, complexBean, 0);

		HGHandle toBeRemoved = node.add(10, intType, 0);
		HGHandle toBeReplaced = node.add(Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6 }), listType, 0);

		ArrayList<Integer> ints = new ArrayList<Integer>();
		for (int i = 0; i < 37; i++)
		{
			ints.add(i);
			node.add(i, intType, 0);
		}

		node.replace(toBeReplaced, ints, listType);

		Assert.assertTrue(node.remove(toBeRemoved));
		assertEquals(node.count(hg.eq(10)), 1);
		assertEquals(node.get(x1), "Hello World");
		assertEquals(node.get(x2), complexBean);

		HGSearchResult<HGHandle> rs = node.find(hg.type(intType));
		Set<Integer> intSet = new HashSet<Integer>();
		intSet.addAll(ints);
		ArrayList<HGHandle> intHandles = new ArrayList<HGHandle>();
		while (rs.hasNext())
		{
			Assert.assertTrue(intSet.contains(node.get(rs.next())));
			intSet.remove(node.get(rs.current()));
			intHandles.add(rs.current());
			if (rs.hasPrev())
			{
				rs.prev();
				rs.next();
			}
		}
		rs.close();
		Assert.assertTrue(intSet.isEmpty());
		equalArrays(node.findAll(hg.type(intType)).toArray(new HGHandle[0]), intHandles.toArray(new HGHandle[0]));
		Integer[] II = node.getAll(hg.type(intType)).toArray(new Integer[0]);
        equalArrays(II, ints.toArray(new Integer[0]));
		assertEquals(node.get(toBeReplaced), ints);
	}

	public static void main(String[] argv)
	{
		TestCACT test = new TestCACT();
		try
		{
			test.setUp();
			// if (test.which || test.both)
			// test.testRemoteQuery();
			// else
			// T.sleep(1000*60*60*5);
			while (test.peer2.getConnectedPeers().isEmpty() || test.peer1.getConnectedPeers().isEmpty())
				Thread.sleep(500);

			test.testHyperNode();
			System.out.println("test passed successfully");
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
		}
		finally
		{
			test.tearDown();
		}
	}
}