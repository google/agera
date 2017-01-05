package android.databinding;

import android.view.View;
import java.util.HashMap;
import java.util.Map;

//TODO remove this hack when moving to robolectric 3.1
public final class DataBinderMapper {
  private static Map<Integer, ViewDataBinding> bindings = new HashMap<>();
  public static final int TARGET_MIN_SDK = 14;

  public int getLayoutId(String string) {
    return 1;
  }

  public static void setDataBinding(ViewDataBinding dataBinding, int layoutId) {
    bindings.put(layoutId, dataBinding);
  }

  public ViewDataBinding getDataBinder(
      android.databinding.DataBindingComponent component, View view, int layoutId) {
    return bindings.get(layoutId);
  }
}
