package com.javamex.classmexer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class MemoryUtil
{
  public static enum VisibilityFilter
  {
    ALL,  PRIVATE_ONLY,  NON_PUBLIC,  NONE;
  }
  
  public static long memoryUsageOf(Object obj)
  {
    return Agent.getInstrumentation().getObjectSize(obj);
  }
  
  public static long deepMemoryUsageOf(Object obj)
  {
    return deepMemoryUsageOf(obj, VisibilityFilter.NON_PUBLIC);
  }
  
  public static long deepMemoryUsageOf(Object obj, VisibilityFilter referenceFilter)
  {
    return deepMemoryUsageOf0(Agent.getInstrumentation(), new HashSet(), obj, referenceFilter);
  }
  
  public static long deepMemoryUsageOfAll(Collection<? extends Object> objs)
  {
    return deepMemoryUsageOfAll(objs, VisibilityFilter.NON_PUBLIC);
  }
  
  public static long deepMemoryUsageOfAll(Collection<? extends Object> objs, VisibilityFilter referenceFilter)
  {
    Instrumentation instr = Agent.getInstrumentation();
    long total = 0L;
    
    Set<Integer> counted = new HashSet(objs.size() * 4);
    for (Object o : objs) {
      total += deepMemoryUsageOf0(instr, counted, o, referenceFilter);
    }
    return total;
  }
  
  private static long deepMemoryUsageOf0(Instrumentation instrumentation, Set<Integer> counted, Object obj, VisibilityFilter filter)
    throws SecurityException
  {
    Stack<Object> st = new Stack();
    st.push(obj);
    long total = 0L;
    while (!st.isEmpty())
    {
      Object o = st.pop();
      if (counted.add(Integer.valueOf(System.identityHashCode(o))))
      {
        long sz = instrumentation.getObjectSize(o);
        total += sz;
        
        Class clz = o.getClass();
        

        Class compType = clz.getComponentType();
        int localObject1;
        int localObject2;
        Object el;
        if ((compType != null) && 
          (!compType.isPrimitive()))
        {
          Object[] arr = (Object[])o;
          Object[] arrayOfObject1 = arr;localObject1 = 0;
          for (localObject2 = arrayOfObject1.length; localObject1 <localObject2;localObject1++)
          {
            el = arrayOfObject1[localObject1];
            if (el != null) {
              st.push(el);
            }
          }
        }
        while (clz != null)
        {
          for (Field fld : clz.getDeclaredFields())
          {
            int mod = fld.getModifiers();
            if (((mod & 0x8) == 0) && (isOf(filter, mod)))
            {
              Class fieldClass = fld.getType();
              if (!fieldClass.isPrimitive())
              {
                if (!fld.isAccessible()) {
                  fld.setAccessible(true);
                }
                try
                {
                  Object subObj = fld.get(o);
                  if (subObj != null) {
                    st.push(subObj);
                  }
                }
                catch (IllegalAccessException illAcc)
                {
                  throw new InternalError("Couldn't read " + fld);
                }
              }
            }
          }
          clz = clz.getSuperclass();
        }
      }
    }
    return total;
  }
  
  private static boolean isOf(VisibilityFilter f, int mod)
  {
    switch (f)
    {
    case ALL: 
      return true;
    case PRIVATE_ONLY: 
      return false;
    case NONE: 
      return (mod & 0x2) != 0;
    case NON_PUBLIC: 
      return (mod & 0x1) == 0;
    }
    throw new IllegalArgumentException("Illegal filter " + mod);
  }
}
