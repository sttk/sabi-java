package com.github.sttk.sabi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DataSrcListTest {
  private DataSrcListTest() {}

  static class SyncDataSrc implements DataSrc {
    int id;
    boolean willFail;
    List<String> logger;

    SyncDataSrc(int id, List<String> logger, boolean willFail) {
      this.id = id;
      this.willFail = willFail;
      this.logger = logger;
    }

    @Override
    public void setup(AsyncGroup ag) throws Exc {
      if (this.willFail) {
        logger.add(String.format("SyncDataSrc %d failed to setup", this.id));
        throw new Exc("XXX");
      }
      logger.add(String.format("SyncDataSrc %d setupped", this.id));
    }

    @Override
    public void close() {
      logger.add(String.format("SyncDataSrc %d closed", this.id));
    }

    @Override
    public DataConn createDataConn() throws Exc {
      logger.add(String.format("SyncDataSrc %d created DataConn", this.id));
      var conn = new SyncDataConn();
      return conn;
    }
  }

  static class AsyncDataSrc implements DataSrc {
    int id;
    boolean willFail;
    List<String> logger;

    AsyncDataSrc(int id, List<String> logger, boolean willFail) {
      this.id = id;
      this.willFail = willFail;
      this.logger = logger;
    }

    @Override
    public void setup(AsyncGroup ag) throws Exc {
      ag.add(
          () -> {
            try {
              Thread.sleep(50);
            } catch (Exception e) {
            }
            if (this.willFail) {
              logger.add(String.format("AsyncDataSrc %d failed to setup", this.id));
              throw new Exc("XXX");
            }
            logger.add(String.format("AsyncDataSrc %d setupped", this.id));
          });
    }

    @Override
    public void close() {
      logger.add(String.format("AsyncDataSrc %d closed", this.id));
    }

    @Override
    public DataConn createDataConn() throws Exc {
      logger.add(String.format("AsyncDataSrc %d created DataConn", this.id));
      var conn = new AsyncDataConn();
      return conn;
    }
  }

  static class SyncDataConn implements DataConn {
    @Override
    public void commit(AsyncGroup ag) throws Exc {}

    @Override
    public void preCommit(AsyncGroup ag) throws Exc {}

    @Override
    public void postCommit(AsyncGroup ag) {}

    @Override
    public boolean shouldForceBack() {
      return false;
    }

    @Override
    public void rollback(AsyncGroup ag) {}

    @Override
    public void forceBack(AsyncGroup ag) {}

    @Override
    public void close() {}
  }

  static class AsyncDataConn implements DataConn {
    @Override
    public void commit(AsyncGroup ag) throws Exc {}

    @Override
    public void preCommit(AsyncGroup ag) throws Exc {}

    @Override
    public void postCommit(AsyncGroup ag) {}

    @Override
    public boolean shouldForceBack() {
      return false;
    }

    @Override
    public void rollback(AsyncGroup ag) {}

    @Override
    public void forceBack(AsyncGroup ag) {}

    @Override
    public void close() {}
  }

  @Test
  void test_new() {
    var dsList = new DataSrcList(false);

    assertThat(dsList.local).isEqualTo(false);
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();
  }

  @Test
  void test_appendContainerPtrNotSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrNotSetup(ptr1);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr1);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isNull();

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrNotSetup(ptr2);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr2);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isNull();

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeHeadContainerPtrNotSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrNotSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrNotSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrNotSetup(ptr1);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr2);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr2.prev).isNull();
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeMiddleContainerPtrNotSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrNotSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrNotSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrNotSetup(ptr2);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr1);
    assertThat(ptr3.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeLastContainerPtrNotSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrNotSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrNotSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr2);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeAllContainerPtrNotSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrNotSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrNotSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrNotSetup(ptr1);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr2);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr2.prev).isNull();
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrNotSetup(ptr2);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr3);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr3.prev).isNull();
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrNotSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeAndCloseLocalContainerPtrNotSetupByName() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrNotSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrNotSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrNotSetup(ptr3);

    var ds4 = new SyncDataSrc(4, logger, false);
    var ptr4 = new DataSrcContainer(false, "qux", ds4);
    dsList.appendContainerPtrNotSetup(ptr4);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr4);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isEqualTo(ptr4);
    assertThat(ptr4.prev).isEqualTo(ptr3);
    assertThat(ptr4.next).isNull();

    dsList.removeAndCloseContainerPtrNotSetupByName("bar");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr1);
    assertThat(dsList.notSetupLast).isEqualTo(ptr4);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr1);
    assertThat(ptr3.next).isEqualTo(ptr4);
    assertThat(ptr4.prev).isEqualTo(ptr3);
    assertThat(ptr4.next).isNull();

    dsList.removeAndCloseContainerPtrNotSetupByName("foo");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr3);
    assertThat(dsList.notSetupLast).isEqualTo(ptr4);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr3.prev).isNull();
    assertThat(ptr3.next).isEqualTo(ptr4);
    assertThat(ptr4.prev).isEqualTo(ptr3);
    assertThat(ptr4.next).isNull();

    dsList.removeAndCloseContainerPtrNotSetupByName("qux");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isEqualTo(ptr3);
    assertThat(dsList.notSetupLast).isEqualTo(ptr3);
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    assertThat(ptr3.prev).isNull();
    assertThat(ptr3.next).isNull();

    dsList.removeAndCloseContainerPtrNotSetupByName("baz");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    dsList.closeDataSrcs();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc 2 closed",
            "SyncDataSrc 1 closed",
            "SyncDataSrc 4 closed",
            "SyncDataSrc 3 closed");
  }

  @Test
  void test_appendContainerPtrDidSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr1);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isNull();

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr2);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isNull();

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeHeadContainerPtrDidSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrDidSetup(ptr1);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr2);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr2.prev).isNull();
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeMiddleContainerPtrDidSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrDidSetup(ptr2);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr1);
    assertThat(ptr3.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeLastContainerPtrDidSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr2);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeAllContainerPtrDidSetup() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrDidSetup(ptr1);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr2);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr2.prev).isNull();
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrDidSetup(ptr2);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr3);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr3.prev).isNull();
    assertThat(ptr3.next).isNull();

    dsList.removeContainerPtrDidSetup(ptr3);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    dsList.closeDataSrcs();
  }

  @Test
  void test_removeAndCloseLocalContainerPtrDidSetupByName() {
    var dsList = new DataSrcList(false);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    var ds4 = new SyncDataSrc(4, logger, false);
    var ptr4 = new DataSrcContainer(false, "qux", ds4);
    dsList.appendContainerPtrDidSetup(ptr4);

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr4);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr2);
    assertThat(ptr2.prev).isEqualTo(ptr1);
    assertThat(ptr2.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr2);
    assertThat(ptr3.next).isEqualTo(ptr4);
    assertThat(ptr4.prev).isEqualTo(ptr3);
    assertThat(ptr4.next).isNull();

    dsList.removeAndCloseContainerPtrDidSetupByName("bar");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr1);
    assertThat(dsList.didSetupLast).isEqualTo(ptr4);

    assertThat(ptr1.prev).isNull();
    assertThat(ptr1.next).isEqualTo(ptr3);
    assertThat(ptr3.prev).isEqualTo(ptr1);
    assertThat(ptr3.next).isEqualTo(ptr4);
    assertThat(ptr4.prev).isEqualTo(ptr3);
    assertThat(ptr4.next).isNull();

    dsList.removeAndCloseContainerPtrDidSetupByName("foo");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr3);
    assertThat(dsList.didSetupLast).isEqualTo(ptr4);

    assertThat(ptr3.prev).isNull();
    assertThat(ptr3.next).isEqualTo(ptr4);
    assertThat(ptr4.prev).isEqualTo(ptr3);
    assertThat(ptr4.next).isNull();

    dsList.removeAndCloseContainerPtrDidSetupByName("qux");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isEqualTo(ptr3);
    assertThat(dsList.didSetupLast).isEqualTo(ptr3);

    assertThat(ptr3.prev).isNull();
    assertThat(ptr3.next).isNull();

    dsList.removeAndCloseContainerPtrDidSetupByName("baz");

    assertThat(dsList.local).isFalse();
    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.notSetupLast).isNull();
    assertThat(dsList.didSetupHead).isNull();
    assertThat(dsList.didSetupLast).isNull();

    dsList.closeDataSrcs();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc 2 closed",
            "SyncDataSrc 1 closed",
            "SyncDataSrc 4 closed",
            "SyncDataSrc 3 closed");
  }

  @Test
  void test_copyContainerPtrsDidSetupInto() {
    var dsList = new DataSrcList(false);

    var m = new HashMap<String, DataSrcContainer>();
    dsList.copyContainerPtrsDidSetupInto(m);
    assertThat(m).hasSize(0);

    var logger = new ArrayList<String>();

    var ds1 = new SyncDataSrc(1, logger, false);
    var ptr1 = new DataSrcContainer(false, "foo", ds1);
    dsList.appendContainerPtrDidSetup(ptr1);

    var ds2 = new SyncDataSrc(2, logger, false);
    var ptr2 = new DataSrcContainer(false, "bar", ds2);
    dsList.appendContainerPtrDidSetup(ptr2);

    var ds3 = new SyncDataSrc(3, logger, false);
    var ptr3 = new DataSrcContainer(false, "baz", ds3);
    dsList.appendContainerPtrDidSetup(ptr3);

    m = new HashMap<String, DataSrcContainer>();
    dsList.copyContainerPtrsDidSetupInto(m);

    assertThat(m).hasSize(3);
    assertThat(m.get("foo")).isEqualTo(ptr1);
    assertThat(m.get("bar")).isEqualTo(ptr2);
    assertThat(m.get("baz")).isEqualTo(ptr3);

    dsList.closeDataSrcs();
  }

  @Test
  void test_setupAndCreateDataConnAndClose() {
    var logger = new ArrayList<String>();

    var dsList = new DataSrcList(false);

    var dsAsync = new AsyncDataSrc(1, logger, false);
    dsList.addDataSrc("foo", dsAsync);

    var dsSync = new SyncDataSrc(2, logger, false);
    dsList.addDataSrc("bar", dsSync);

    var errMap = dsList.setupDataSrcs();
    assertThat(errMap).isEmpty();

    var ptr = dsList.didSetupHead;
    try {
      var conn = ptr.ds.createDataConn();
      assertThat(conn).isNotNull();
    } catch (Exc e) {
      fail(e);
    }

    ptr = ptr.next;
    try {
      var conn = ptr.ds.createDataConn();
      assertThat(conn).isNotNull();
    } catch (Exc e) {
      fail(e);
    }

    dsList.closeDataSrcs();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc 2 setupped",
            "AsyncDataSrc 1 setupped",
            "AsyncDataSrc 1 created DataConn",
            "SyncDataSrc 2 created DataConn",
            "SyncDataSrc 2 closed",
            "AsyncDataSrc 1 closed");
  }

  @Test
  void test_failToSetupSyncAndClose() {
    var logger = new ArrayList<String>();

    var dsList = new DataSrcList(true);

    var dsAsync = new AsyncDataSrc(1, logger, false);
    dsList.addDataSrc("foo", dsAsync);

    var dsSync = new SyncDataSrc(2, logger, true);
    dsList.addDataSrc("bar", dsSync);

    var errMap = dsList.setupDataSrcs();
    assertThat(errMap).hasSize(1);

    var err = errMap.get("bar");
    assertThat(err.getReason()).isEqualTo("XXX");

    dsList.closeDataSrcs();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc 2 failed to setup", "AsyncDataSrc 1 setupped", "AsyncDataSrc 1 closed");
  }

  @Test
  void test_failToSetupAsyncAndClose() {
    var logger = new ArrayList<String>();

    var dsList = new DataSrcList(true);

    var dsAsync = new AsyncDataSrc(1, logger, true);
    dsList.addDataSrc("foo", dsAsync);

    var dsSync = new SyncDataSrc(2, logger, false);
    dsList.addDataSrc("bar", dsSync);

    var excMap = dsList.setupDataSrcs();
    assertThat(excMap).hasSize(1);

    var exc = excMap.get("foo");
    assertThat(exc.getReason()).isEqualTo("XXX");

    dsList.closeDataSrcs();

    assertThat(logger)
        .containsExactly(
            "SyncDataSrc 2 setupped", "AsyncDataSrc 1 failed to setup", "SyncDataSrc 2 closed");
  }

  @Test
  void test_noDataSrc() {
    var dsList = new DataSrcList(false);

    var excMap = dsList.setupDataSrcs();
    assertThat(excMap).hasSize(0);

    dsList.closeDataSrcs();

    assertThat(dsList.notSetupHead).isNull();
    assertThat(dsList.didSetupHead).isNull();
  }
}
