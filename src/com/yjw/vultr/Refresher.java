package com.yjw.vultr;
import java.util.*;
// Tiered refresh bookkeeping: which resource is loading, and whether its cached copy is old enough to fetch again.
public final class Refresher {
 public static final long INSTANCES=60_000L,ACCOUNT=5*60_000L,CLOUD=15*60_000L,COSTS=6*3600_000L;
 private final Map<String,Long> attempted=new HashMap<>();private final Set<String> running=new HashSet<>();
 // checked: when the persisted copy was fetched (0 if never). A failed attempt also waits one ttl before automatic retry.
 public synchronized boolean due(String key,long ttl,long checked,long now){if(running.contains(key))return false;long last=Math.max(checked,attempted.getOrDefault(key,0L));return last<=0||now<last||now-last>=ttl;}
 public synchronized boolean start(String key){return running.add(key);}
 public synchronized void finish(String key,long now){running.remove(key);attempted.put(key,now);}
 public synchronized boolean running(String key){return running.contains(key);}
 public synchronized boolean idle(){return running.isEmpty();}
}
