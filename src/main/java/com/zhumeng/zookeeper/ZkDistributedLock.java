package com.zhumeng.zookeeper;

import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * @author
 * @version
 * 2018��4��28�� ����11:10:01
 */

/**
 * ������: ZkDistributedLock
 * ������: 
 * ������: zhangdehai
 * ����ʱ��:2018��4��28�� ����11:10:01
 */
public class ZkDistributedLock {  
  
    // ��һ����̬������ģ�⹫����Դ  
    private static int counter = 0;  
  
    public static void plus() {  
  
        // ��������һ  
        counter++;  
  
        // �߳�������������룬ģ����ʵ�еķ�ʱ����  
        int sleepMillis = (int) (Math.random() * 100);  
        try {  
            Thread.sleep(sleepMillis);  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }  
    }  
  
    // �߳�ʵ����  
    static class CountPlus extends Thread {  
  
        private static final String LOCK_ROOT_PATH = "/Locks";  
        private static final String LOCK_NODE_NAME = "Lock_";  
  
        // ÿ���̳߳���һ��zk�ͻ��ˣ������ȡ�����ͷ���  
        ZooKeeper zkClient;  
  
        @Override  
        public void run() {  
  
            for (int i = 0; i < 20; i++) {  
  
                // ���ʼ�����֮ǰ��Ҫ�Ȼ�ȡ��  
                String path = getLock();  
  
                // ִ������  
                plus();  
  
                // ִ����������ͷ���  
                releaseLock(path);  
            }  
              
            closeZkClient();  
            System.out.println(Thread.currentThread().getName() + "ִ����ϣ�" + counter);  
        }  
  
        /** 
         * ��ȡ�����������ӽڵ㣬���ýڵ��Ϊ�����С�Ľڵ�ʱ���ȡ�� 
         */  
        private String getLock() {  
            try {  
                // ����EPHEMERAL_SEQUENTIAL���ͽڵ�  
                String lockPath = zkClient.create(LOCK_ROOT_PATH + "/" + LOCK_NODE_NAME,  
                        Thread.currentThread().getName().getBytes(), Ids.OPEN_ACL_UNSAFE,  
                        CreateMode.EPHEMERAL_SEQUENTIAL);  
                System.out.println(Thread.currentThread().getName() + " create path : " + lockPath);  
  
                // ���Ի�ȡ��  
                tryLock(lockPath);  
  
                return lockPath;  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
            return null;  
        }  
  
        /** 
         * �ú�����һ���ݹ麯�� ����������ֱ�ӷ��أ����������̣߳��ȴ���һ���ڵ��ͷ�������Ϣ��Ȼ������tryLock 
         */  
        private boolean tryLock(String lockPath) throws KeeperException, InterruptedException {  
  
            // ��ȡLOCK_ROOT_PATH�����е��ӽڵ㣬�����սڵ��������  
            List<String> lockPaths = zkClient.getChildren(LOCK_ROOT_PATH, false);  
            Collections.sort(lockPaths);  
  
            int index = lockPaths.indexOf(lockPath.substring(LOCK_ROOT_PATH.length() + 1));  
            if (index == 0) { // lockPath�������С�Ľڵ㣬���ȡ��  
                System.out.println(Thread.currentThread().getName() + " get lock, lockPath: " + lockPath);  
                return true;  
            } else { // lockPath���������С�Ľڵ�  
  
                // ����Watcher�����lockPath��ǰһ���ڵ�  
                Watcher watcher = new Watcher() {  
                    public void process(WatchedEvent event) {  
                        System.out.println(event.getPath() + " has been deleted");  
                        synchronized (this) {  
                            notifyAll();  
                        }  
                    }  
                };  
                String preLockPath = lockPaths.get(index - 1);  
                //
                Stat stat = zkClient.exists(LOCK_ROOT_PATH + "/" + preLockPath, watcher);  
  
                if (stat == null) { // ����ĳ��ԭ��ǰһ���ڵ㲻�����ˣ��������ӶϿ���������tryLock  
                    return tryLock(lockPath);  
                } else { // ������ǰ���̣�ֱ��preLockPath�ͷ���������tryLock  
                    System.out.println(Thread.currentThread().getName() + " wait for " + preLockPath);  
                    synchronized (watcher) {  
                        watcher.wait();  
                    }  
                    return tryLock(lockPath);  
                }  
            }  
  
        }  
  
        /** 
         * �ͷ�������ɾ��lockPath�ڵ� 
         */  
        private void releaseLock(String lockPath) {  
            try {  
                zkClient.delete(lockPath, -1);  
            } catch (InterruptedException | KeeperException e) {  
                e.printStackTrace();  
            }  
        }  
  
        public void setZkClient(ZooKeeper zkClient) {  
            this.zkClient = zkClient;  
        }  
          
        public void closeZkClient(){  
            try {  
                zkClient.close();  
            } catch (InterruptedException e) {  
                e.printStackTrace();  
            }  
        }  
  
        public CountPlus(String threadName) {  
            super(threadName);  
        }  
    }  
  
    public static void main(String[] args) throws Exception {  
//    	 ZooKeeper zkClient = new ZooKeeper("127.0.0.1:2181", 3000, null);  
//    	 zkClient.create("/Locks", "ss".getBytes(), Ids.OPEN_ACL_UNSAFE,  
//                 CreateMode.PERSISTENT);
        // ��������߳�  
        CountPlus threadA = new CountPlus("threadA");  
        setZkClient(threadA);  
        threadA.start();  
  
        CountPlus threadB = new CountPlus("threadB");  
        setZkClient(threadB);  
        threadB.start();  
  
        CountPlus threadC = new CountPlus("threadC");  
        setZkClient(threadC);  
        threadC.start();  
  
        CountPlus threadD = new CountPlus("threadD");  
        setZkClient(threadD);  
        threadD.start();  
  
        CountPlus threadE = new CountPlus("threadE");  
        setZkClient(threadE);  
        threadE.start();  
    }  
  
    public static void setZkClient(CountPlus thread) throws Exception {  
        ZooKeeper zkClient = new ZooKeeper("127.0.0.1:2181", 3000, null);  
        thread.setZkClient(zkClient);  
    }  
  
}  