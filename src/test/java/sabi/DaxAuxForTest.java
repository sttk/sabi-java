package sabi;

import java.util.Map;

public class DaxAuxForTest {

  static void clearGlobalDaxSrcs() throws Exception {
    final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
    f0.setAccessible(true);
    f0.setBoolean(null, false);

    final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
    f1.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map1 = (Map<String, DaxSrc>) f1.get(null);
    map1.clear();
  }

  static boolean isGlobalDaxSrcsFixed() throws Exception {
    final var f0 = DaxBase.class.getDeclaredField("isGlobalDaxSrcsFixed");
    f0.setAccessible(true);
    return f0.getBoolean(null);
  }

  static Map<String, DaxSrc> globalDaxSrcMap() throws Exception {
    final var f1 = DaxBase.class.getDeclaredField("globalDaxSrcMap");
    f1.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map1 = (Map<String, DaxSrc>) f1.get(null);
    return map1;
  }

  static boolean isLocalDaxSrcsFixed(DaxBase base) throws Exception {
    final var f0 = DaxBase.class.getDeclaredField("isLocalDaxSrcsFixed");
    f0.setAccessible(true);
    return f0.getBoolean(base);
  }

  static Map<String, DaxSrc> localDaxSrcMap(DaxBase base) throws Exception {
    final var f1 = DaxBase.class.getDeclaredField("localDaxSrcMap");
    f1.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map1 = (Map<String, DaxSrc>) f1.get(base);
    return map1;
  }

  static Map<String, DaxSrc> daxConnMap(DaxBase base) throws Exception {
    final var f1 = DaxBase.class.getDeclaredField("daxConnMap");
    f1.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map1 = (Map<String, DaxSrc>) f1.get(base);
    return map1;
  }
}
