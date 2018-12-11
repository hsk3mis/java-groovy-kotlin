package com.gryglicki.gc;

import org.junit.Before;
import org.junit.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GCTest {

    static AtomicBoolean finalized;

    @Before
    public void setUp() {
        GCTest.finalized = new AtomicBoolean();
    }

    static class LargeObject {
        int[] array = new int[1_000_000];

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            System.out.println("Finzalization of " + this.getClass());
            GCTest.finalized.set(true);
        }
    }

    @Test
    public void should_call_finalize() {
        //Given
        LargeObject o = new LargeObject();
        System.out.println(o.toString()); //side effect to prevent JIT removing the object

        //When
        o = null;
        gcAndWait();

        //Then
        assertTrue(finalized.get());
    }

    @Test
    public void should_usually_remove_weak_references() {
        //Given
        WeakReference<LargeObject> weakRef = new WeakReference<>(new LargeObject());
        System.out.println(weakRef.get().toString());

        //When
        gcAndWait();

        //Then
        assertTrue(finalized.get());
    }

    @Test
    public void should_usually_not_remove_soft_references() {
        //Given
        SoftReference<LargeObject> softRef = new SoftReference<>(new LargeObject());
        System.out.println(softRef.get().toString());

        //When
        gcAndWait();

        //Then
        assertFalse(finalized.get());
    }

    @Test
    public void should_fill_ReferenceQueue_with_garbage_collected_references() {
        //Given
        ReferenceQueue<LargeObject> refQueue = new ReferenceQueue<>();
        WeakReference<LargeObject> weakRef = new WeakReference<>(new LargeObject(), refQueue);
        System.out.println(weakRef.get().toString());

        //When
        gcAndWait();

        //Then
        assertTrue(finalized.get());
        assertSame(weakRef, refQueue.poll());
    }

    @Test
    public void should_fill_ReferenceQueue_with_garbage_collected_references_only_after_finalization() {
        //Given
        LargeObject largeObject = new LargeObject();
        ReferenceQueue<LargeObject> refQueue = new ReferenceQueue<>();
        WeakReference<LargeObject> weakRef = new WeakReference<>(largeObject, refQueue);
        System.out.println(weakRef.get().toString());

        //When
        gcAndWait(); //no finalization, because largeObject is strongly referenced from process stack
        //Then
        assertFalse(finalized.get());
        assertNull(refQueue.poll());

        //When
        largeObject = null;
        gcAndWait();
        //Then
        assertTrue(finalized.get());
        assertSame(weakRef, refQueue.poll());
    }

    private void gcAndWait() {
        try {
            System.gc();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
