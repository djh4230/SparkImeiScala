package com.llyt.util;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/** 
 * @author 作者 E-mail:Dengjh@yintong.cn.com 
 * @version 创建时间：2015-7-29 上午11:57:27
 */

public class CounterMap {
	  HashMap<String, Counter> map = new HashMap<String, Counter>();
	  
	  public void increment(String key, long increment) {
	    Counter count = map.get(key);
	    if (count == null) {
	      count = new Counter();
	      map.put(key, count);
	    } 
	    count.value += increment;
	  }
	  
	  
	  public long getValue(String key) {
	    Counter count = map.get(key);
	    if (count != null) {
	      return count.value;
	    } else {
	      return 0;
	    }
	  }
	  
	  public Set<Entry<String, Counter>> entrySet() {
	    return map.entrySet();
	  }
	  
	  public void clear() {
	    map.clear();
	  }
	  
	  public static class Counter {
	    public long value;
	  }
	  
	  
	}
