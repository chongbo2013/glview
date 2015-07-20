package com.glview.hwui.cache;

import com.glview.pool.Pool;
import com.glview.pool.Poolable;

/**
 * 资源的引用计数
 * 
 * @author lijing.lj
 */
public class ResourceReference implements Poolable {
	
	static Pool<ResourceReference> sPool = new Pool<ResourceReference>(false, 100);
	
	Object target;
	int refCount;
	
	private ResourceReference() {
	}
	
	public void recycle() {
		target = null;
		refCount = 0;
		sPool.push(this);
	}
	
	public static ResourceReference obtain(Object target) {
		ResourceReference o = (ResourceReference) sPool.poll(ResourceReference.class);
		if (o == null) {
			o = new ResourceReference();
		}
		o.target = target;
		o.refCount = 0;
		return o;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ResourceReference)) {
			return false;
		}
		ResourceReference other = (ResourceReference) o;
		if (other.target == null || target == null) {
			return super.equals(o);
		}
		return other.target.equals(target);
	}
	
	public void increaseReferenceCount() {
		refCount ++;
	}
	
	public void decreaseReferenceCount() {
		refCount --;
	}
	
	@Override
	public int hashCode() {
		return target != null ? target.hashCode() : super.hashCode();
	}

}
