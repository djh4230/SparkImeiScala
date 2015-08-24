package com.llyt.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;




/**
 * @author 作者 E-mail:Dengjh@yintong.cn.com
 * @version 创建时间：2015-7-29 下午2:29:05
 */

public class HbaseUtil {
	static HbaseUtil singleton;
	static String tableName;
	static String columnFamily;
	static HTable hTable;
	static long lastUsed;
	static long flushInterval;
	static CloserThread closerThread;
	static FlushThread flushThread;
	static Object locker = new Object();
	static String ZOOKEEPER = "192.168.161.9:2181,192.168.161.10:2181,192.168.161.11:2181";

	private HbaseUtil(String tableName, String columnFamily) {
		this.tableName = tableName;
		this.columnFamily = columnFamily;
	}

	public static HbaseUtil getInstance(String tableName, String columnFamily) {

		if (singleton == null) {
			synchronized (locker) {
				if (singleton == null) {
					singleton = new HbaseUtil(tableName, columnFamily);
					initialize();
				}
			}
		}
		return singleton;
	}
	
	public static HTable getTable(String tableName, String columnFamily){
		if (hTable==null) {
			initialize();
		}
		hTable.setAutoFlush(true);
		return hTable;
	}
	private static void initialize() {
		if (hTable == null) {
			synchronized (locker) {
				if (hTable == null) {
					Configuration hConfig = HBaseConfiguration.create();
					hConfig.set("hbase.zookeeper.quorum", ZOOKEEPER);
					try {
						hTable = new HTable(hConfig, tableName);
						updateLastUsed();

					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					flushThread = new FlushThread(flushInterval);
					//flushThread.start();
					closerThread = new CloserThread();
					//closerThread.start();
				}
			}
		}
	}

	private static void updateLastUsed() {
		lastUsed = System.currentTimeMillis();
	}
	
	protected static class FlushThread extends Thread {
	    long sleepTime;
	    boolean continueLoop = true;

	    public FlushThread(long sleepTime) {
	      this.sleepTime = sleepTime;
	    }

	    @Override
	    public void run() {
	      while (continueLoop) {
	        try {
	          Thread.sleep(sleepTime);
	        } catch (InterruptedException e) {
	          e.printStackTrace();
	        }
	      }
	    }

	    public void stopLoop() {
	      continueLoop = false;
	    }
	  }
	public static class CloserThread extends Thread {

	    boolean continueLoop = true;

	    @Override
	    public void run() {
	      while (continueLoop) {

	        if (System.currentTimeMillis() - lastUsed > 30000) {
	          singleton.close();
	          break;
	        }

	        try {
	          Thread.sleep(60000);
	        } catch (InterruptedException e) {
	          e.printStackTrace();
	        }
	      }
	    }

	    public void stopLoop() {
	      continueLoop = false;
	    }
	  }
	protected void close() {
	    if (hTable != null) {
	      synchronized (locker) {
	        if (hTable != null) {
	          if (hTable != null && System.currentTimeMillis() - lastUsed > 30000) {
	            flushThread.stopLoop();
	            flushThread = null;
	            try {
	              hTable.close();
	            } catch (IOException e) {
	              e.printStackTrace();
	            }
	            hTable = null;
	          }
	        }
	      }
	    }
	  }
	public void put(String rowKey,
			Map<String, String> map) throws IOException {
		ArrayList<Put> list=new ArrayList<Put>();
		for (Entry<String, String> entry : map.entrySet()) {
			Put put = new Put(Bytes.toBytes(rowKey));
			put.add(Bytes.toBytes(columnFamily),
					Bytes.toBytes(entry.getKey()),
					Bytes.toBytes(entry.getValue()));
			list.add(put);
		}
		if(hTable==null)
			initialize();
		 hTable.put(list);
		 
	}
	public void put1(String rowKey,
			Map<String, Integer> map) throws IOException {
		Map<String, String> m=new HashMap<String, String>();
		
		for(Entry<String, Integer> entry:map.entrySet()){
			String k=entry.getKey();
			String value=entry.getValue()+"";
			m.put(k, value);
		}
		put(rowKey, m);
	}
	public void scan() throws IOException{
		ResultScanner rs=null;
		Scan scan=new Scan();
		scan.addFamily(Bytes.toBytes(columnFamily));
		if(hTable==null)
			initialize();
		try {
			rs=hTable.getScanner(scan);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (Result r = rs.next(); r != null; r = rs.next()) {
			//byte[] row=r.getRow();
			List<Cell> cells=r.listCells();
			//byte[] value = r.getValue(columnFamily.getBytes(), "搜索引擎".getBytes());
			//String m = new String(value);
			//System.out.println("Found row: " + m);
			for(Cell cell:cells){
				
				System.out.println(new String(cell.getQualifier())+":"+new String(cell.getValue()));
			}
		}
	}
	public static void main(String[] args) {
		Map<String, String> map=new HashMap<String, String>();
		map.put("应用软件", "21");
		map.put("yingyongruanjian", "22");
		map.put("搜索引擎", "美团");
		HbaseUtil hbaseUtil=HbaseUtil.getInstance("imei", "imei");
		try {
			//hbaseUtil.put("123", map);
			hbaseUtil.scan();
		} catch (RetriesExhaustedWithDetailsException e) {
			e.printStackTrace();
		} catch (InterruptedIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
