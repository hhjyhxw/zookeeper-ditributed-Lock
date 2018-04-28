/**
 * @author
 * @version
 * 2018��4��28�� ����11:16:03
 */
package com.zhumeng.zookeeper;

import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * ������: ZkTest
 * ������: 
 * ������: zhangdehai
 * ����ʱ��:2018��4��28�� ����11:16:03
 */
public class ZkTest {

	private static final String CONNECT_STRING = "127.0.0.1:2181";
	private static final int SESSION_TIMEOUT = 3000;

	public static void main(String[] args) throws Exception {

		// ����һ��������нڵ�仯��Watcher
		Watcher allChangeWatcher = new Watcher() {
			public void process(WatchedEvent event) {
				System.out.println("**watcher receive WatchedEvent** changed path: " + event.getPath()
						+ "; changed type: " + event.getType().name());
			}
		};

		// ��ʼ��һ����ZK���ӡ�����������
		// 1��Ҫ���ӵķ�������ַ��"IP:port"��ʽ��
		// 2���Ự��ʱʱ��
		// 3���ڵ�仯������
		ZooKeeper zk = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT, allChangeWatcher);

		// �½��ڵ㡣�ĸ�������1���ڵ�·����2���ڵ����ݣ�3���ڵ�Ȩ�ޣ�4������ģʽ
		zk.create("/myName", "chenlongfei".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		System.out.println("create new node '/myName'");

		// �ж�ĳ·���Ƿ���ڡ�����������1���ڵ�·����2���Ƿ��أ�Watcher����ʼ��ZooKeeperʱ�����Watcher��
		Stat beforSstat = zk.exists("/myName", true);
		System.out.println("Stat of '/myName' before change : " + beforSstat.toString());

		// �޸Ľڵ����ݡ�����������1���ڵ�·����2�������ݣ�3���汾�����Ϊ-1����ƥ���κΰ汾
		Stat afterStat = zk.setData("/myName", "clf".getBytes(), -1);
		System.out.println("Stat of '/myName' after change: " + afterStat.toString());

		// ��ȡ�����ӽڵ㡣����������1���ڵ�·����2���Ƿ��ظýڵ�
		List<String> children = zk.getChildren("/", true);
		System.out.println("children of path '/': " + children.toString());

		// ��ȡ�ڵ����ݡ�����������1���ڵ�·����2������ظýڵ㣻3���汾����Ϣ����ͨ��һ��Stat������ָ��
		byte[] nameByte = zk.getData("/myName", true, null);
		String name = new String(nameByte, "UTF-8");
		System.out.println("get data from '/myName': " + name);

		// ɾ���ڵ㡣����������1���ڵ�·����2�� �汾��-1����ƥ���κΰ汾����ɾ����������
		zk.delete("/myName", -1);
		System.out.println("delete '/myName'");

		zk.close();
	}
}