package d13n.util;

import java.util.Comparator;
import java.util.Map;

public class MapValueComparator implements Comparator<Object> {

  Map<?, ?> base;
  public MapValueComparator(Map<?, ?> base) {
      this.base = base;
  }

  public int compare(Object a, Object b) {

    if((Double)base.get(a) > (Double)base.get(b)) {
      return 1;
    } else if((Double)base.get(a) == (Double)base.get(b)) {
      return 0;
    } else {
      return -1;
    }
  }
}
