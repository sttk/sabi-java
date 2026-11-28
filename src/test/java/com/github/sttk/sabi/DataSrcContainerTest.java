package com.github.sttk.sabi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.TestDoubles.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DataSrcContainerTest {
  private DataSrcContainerTest() {}

  @Nested
  class TestConstructor {
    @Test
    void testNameIsNull() {
      var logger = new ArrayList<String>();
      var ds = new SyncDataSrc(1, logger, Fail.None);

      try {
        new DataSrcContainer(true, null, ds);
        fail();
      } catch (NullPointerException e) {
        assertThat(e.getMessage()).isEqualTo("name");
      }
    }

    @Test
    void testDsIsNull() {
      try {
        new DataSrcContainer(true, "foo", null);
        fail();
      } catch (NullPointerException e) {
        assertThat(e.getMessage()).isEqualTo("DataSrc");
      }
    }
  }

  @Nested
  class TestCreateDataConn {
    @Test
    void testCreate() {
      var logger = new ArrayList<String>();

      var ds = new SyncDataSrc(1, logger, Fail.None);
      var dsCont = new DataSrcContainer(true, "foo", ds);

      try {
        SyncDataConn conn = dsCont.createDataConn(SyncDataConn.class);
        assertThat(conn).isNotNull();
      } catch (Err err) {
        fail(err);
      }
    }

    @Test
    void testFailToCreate() {
      var logger = new ArrayList<String>();

      var ds = new SyncDataSrc(1, logger, Fail.CreateDataConn);
      var dsCont = new DataSrcContainer(true, "foo", ds);

      try {
        SyncDataConn conn = dsCont.createDataConn(SyncDataConn.class);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataSrcError.FailToCreateDataConn r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.typeName()).isEqualTo(SyncDataConn.class.getName());
          }
          default -> fail(err);
        }
      }
    }

    @Test
    void testCreateButNull() {
      var logger = new ArrayList<String>();

      var ds = new SyncDataSrc(1, logger, Fail.CreateDataConnButNull);
      var dsCont = new DataSrcContainer(true, "foo", ds);

      try {
        SyncDataConn conn = dsCont.createDataConn(SyncDataConn.class);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataSrcError.CreateDataConnIsNull r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.typeName()).isEqualTo(SyncDataConn.class.getName());
          }
          default -> fail(err);
        }
      }
    }

    @Test
    void testFailToCast() {
      var logger = new ArrayList<String>();

      var ds = new SyncDataSrc(1, logger, Fail.None);
      var dsCont = new DataSrcContainer(true, "foo", ds);

      try {
        var conn = dsCont.createDataConn(AsyncDataConn.class);
        fail();
      } catch (Err err) {
        switch (err.getReason()) {
          case DataSrcError.FailToCastCreatedDataConn r -> {
            assertThat(r.name()).isEqualTo("foo");
            assertThat(r.fromTypeName()).isEqualTo(SyncDataConn.class.getName());
            assertThat(r.toTypeName()).isEqualTo(AsyncDataConn.class.getName());
          }
          default -> fail(err);
        }
      }
    }

    @Test
    void testFailToCastAndCloseError() {
      var logger = new ArrayList<String>();

      var ds = new SyncDataSrc(1, logger, Fail.CloseError);
      var dsCont = new DataSrcContainer(true, "foo", ds);

      try {
        dsCont.createDataConn(AsyncDataConn.class);
        fail();
      } catch (Err err) {
        fail(err);
      } catch (Error e) {
        assertThat(e.getMessage()).isEqualTo("close Error");
      }
    }

    @Test
    void testFailToCastAndCloseRuntimeException() {
      var logger = new ArrayList<String>();

      var ds = new SyncDataSrc(1, logger, Fail.CloseRuntimeException);
      var dsCont = new DataSrcContainer(true, "foo", ds);

      try {
        dsCont.createDataConn(AsyncDataConn.class);
        fail();
      } catch (Err err) {
        fail(err);
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).isEqualTo("close RuntimeException");
      }
    }
  }
}
