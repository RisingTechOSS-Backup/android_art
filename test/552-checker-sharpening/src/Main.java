/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Main {

  public static void assertIntEquals(int expected, int result) {
    if (expected != result) {
      throw new Error("Expected: " + expected + ", found: " + result);
    }
  }

  public static void assertStringEquals(String expected, String result) {
    if (expected != null ? !expected.equals(result) : result != null) {
      throw new Error("Expected: " + expected + ", found: " + result);
    }
  }

  public static void assertClassEquals(Class<?> expected, Class<?> result) {
    if (expected != result) {
      throw new Error("Expected: " + expected + ", found: " + result);
    }
  }

  private static int $noinline$foo(int x) {
    return x;
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: int Main.testSimple(int) builder (after)
  /// CHECK:                InvokeStaticOrDirect method_load_kind:BssEntry

  /// CHECK-START-X86: int Main.testSimple(int) pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testSimple(int) pc_relative_fixups_x86 (after)
  /// CHECK:                X86ComputeBaseMethodAddress
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  public static int testSimple(int x) {
    // This call should use PC-relative .bss array load to retrieve the target method.
    return $noinline$foo(x);
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: int Main.testSimpleAppImage(int) builder (after)
  /// CHECK:                InvokeStaticOrDirect method_load_kind:AppImageRelRo

  /// CHECK-START-X86: int Main.testSimpleAppImage(int) pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testSimpleAppImage(int) pc_relative_fixups_x86 (after)
  /// CHECK:                X86ComputeBaseMethodAddress
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  public static int testSimpleAppImage(int x) {
    // This call should use PC-relative .data.img.rel.ro array load to retrieve the target method.
    return AppImageClass.$noinline$foo(x);
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: int Main.testDiamond(boolean, int) builder (after)
  /// CHECK:                InvokeStaticOrDirect method_load_kind:BssEntry
  /// CHECK:                InvokeStaticOrDirect method_load_kind:BssEntry

  /// CHECK-START-X86: int Main.testDiamond(boolean, int) pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testDiamond(boolean, int) pc_relative_fixups_x86 (after)
  /// CHECK:                X86ComputeBaseMethodAddress
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testDiamond(boolean, int) pc_relative_fixups_x86 (after)
  /// CHECK:                X86ComputeBaseMethodAddress
  /// CHECK-NEXT:           If

  public static int testDiamond(boolean negate, int x) {
    // These calls should use PC-relative loads to retrieve the target method.
    // PC-relative bases used by X86 should be pulled before the If.
    if (negate) {
      return $noinline$foo(-x);
    } else {
      return $noinline$foo(x);
    }
  }

  /// CHECK-START-X86: int Main.testLoop(int[], int) pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testLoop(int[], int) pc_relative_fixups_x86 (after)
  /// CHECK:                X86ComputeBaseMethodAddress
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testLoop(int[], int) pc_relative_fixups_x86 (after)
  /// CHECK:                InvokeStaticOrDirect
  /// CHECK-NOT:            InvokeStaticOrDirect

  /// CHECK-START-X86: int Main.testLoop(int[], int) pc_relative_fixups_x86 (after)
  /// CHECK:                ArrayLength
  /// CHECK-NEXT:           X86ComputeBaseMethodAddress
  /// CHECK-NEXT:           Goto
  /// CHECK:                begin_block
  /// CHECK:                InvokeStaticOrDirect method_load_kind:BssEntry

  public static int testLoop(int[] array, int x) {
    // PC-relative bases used by X86 should be pulled before the loop.
    for (int i : array) {
      x += $noinline$foo(i);
    }
    return x;
  }

  /// CHECK-START-X86: int Main.testLoopWithDiamond(int[], boolean, int) pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: int Main.testLoopWithDiamond(int[], boolean, int) pc_relative_fixups_x86 (after)
  /// CHECK:                If
  /// CHECK:                begin_block
  /// CHECK:                ArrayLength
  /// CHECK-NEXT:           X86ComputeBaseMethodAddress
  /// CHECK-NEXT:           Goto

  public static int testLoopWithDiamond(int[] array, boolean negate, int x) {
    // PC-relative bases used by X86 should be pulled before the loop but not outside the if.
    if (array != null) {
      for (int i : array) {
        if (negate) {
          x += $noinline$foo(-i);
        } else {
          x += $noinline$foo(i);
        }
      }
    }
    return x;
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.String Main.$noinline$getBootImageString() builder (after)
  /// CHECK:                LoadString load_kind:BootImageRelRo

  public static String $noinline$getBootImageString() {
    // Empty string is known to be in the boot image.
    return "";
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.String Main.$noinline$getNonBootImageString() builder (after)
  /// CHECK:                LoadString load_kind:BssEntry

  /// CHECK-START-X86: java.lang.String Main.$noinline$getNonBootImageString() pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: java.lang.String Main.$noinline$getNonBootImageString() pc_relative_fixups_x86 (after)
  /// CHECK-DAG:            X86ComputeBaseMethodAddress
  /// CHECK-DAG:            LoadString load_kind:BssEntry

  public static String $noinline$getNonBootImageString() {
    // This string is not in the boot image.
    return "non-boot-image-string";
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.Class Main.$noinline$getStringClass() builder (after)
  /// CHECK:                LoadClass load_kind:BootImageRelRo class_name:java.lang.String

  public static Class<?> $noinline$getStringClass() {
    // String class is known to be in the boot image.
    return String.class;
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.Class Main.$noinline$getOtherClass() builder (after)
  /// CHECK:                LoadClass load_kind:BssEntry class_name:Other

  /// CHECK-START-X86: java.lang.Class Main.$noinline$getOtherClass() pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: java.lang.Class Main.$noinline$getOtherClass() pc_relative_fixups_x86 (after)
  /// CHECK-DAG:            X86ComputeBaseMethodAddress
  /// CHECK-DAG:            LoadClass load_kind:BssEntry class_name:Other

  public static Class<?> $noinline$getOtherClass() {
    // Other class is neither in the boot image nor in the app image.
    return Other.class;
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.Class Main.$noinline$getAppImageClass() builder (after)
  /// CHECK:                LoadClass load_kind:AppImageRelRo class_name:AppImageClass

  /// CHECK-START-X86: java.lang.Class Main.$noinline$getAppImageClass() pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: java.lang.Class Main.$noinline$getAppImageClass() pc_relative_fixups_x86 (after)
  /// CHECK-DAG:            X86ComputeBaseMethodAddress
  /// CHECK-DAG:            LoadClass load_kind:AppImageRelRo class_name:AppImageClass

  public static Class<?> $noinline$getAppImageClass() {
    // AppImageClass class is in the app image.
    return AppImageClass.class;
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.String Main.$noinline$toHexString(int) builder (after)
  /// CHECK:                InvokeStaticOrDirect method_load_kind:BootImageRelRo
  public static String $noinline$toHexString(int value) {
    return Integer.toString(value, 16);
  }

  /// CHECK-START-{ARM,ARM64,X86,X86_64,RISCV64}: java.lang.String Main.$noinline$toHexStringIndirect(int) builder (after)
  /// CHECK:                InvokeStaticOrDirect method_load_kind:BssEntry

  /// CHECK-START-X86: java.lang.String Main.$noinline$toHexStringIndirect(int) pc_relative_fixups_x86 (before)
  /// CHECK-NOT:            X86ComputeBaseMethodAddress

  /// CHECK-START-X86: java.lang.String Main.$noinline$toHexStringIndirect(int) pc_relative_fixups_x86 (after)
  /// CHECK-DAG:            X86ComputeBaseMethodAddress
  /// CHECK-DAG:            InvokeStaticOrDirect method_load_kind:BssEntry
  public static String $noinline$toHexStringIndirect(int value) {
    return $noinline$toHexString(value);
  }

  public static void main(String[] args) {
    assertIntEquals(1, testSimple(1));
    assertIntEquals(1, testSimpleAppImage(1));
    assertIntEquals(1, testDiamond(false, 1));
    assertIntEquals(-1, testDiamond(true, 1));
    assertIntEquals(3, testLoop(new int[]{ 2 }, 1));
    assertIntEquals(8, testLoop(new int[]{ 3, 4 }, 1));
    assertIntEquals(1, testLoopWithDiamond(null, false, 1));
    assertIntEquals(3, testLoopWithDiamond(new int[]{ 2 }, false, 1));
    assertIntEquals(-6, testLoopWithDiamond(new int[]{ 3, 4 }, true, 1));
    assertStringEquals("", $noinline$getBootImageString());
    assertStringEquals("non-boot-image-string", $noinline$getNonBootImageString());
    assertClassEquals(String.class, $noinline$getStringClass());
    assertClassEquals(Other.class, $noinline$getOtherClass());
    assertStringEquals("12345678", $noinline$toHexString(0x12345678));
    assertStringEquals("76543210", $noinline$toHexStringIndirect(0x76543210));
  }
}

class AppImageClass {
  public static int $noinline$foo(int x) {
    return x;
  }
}

class Other {
}

class MainExtension extends Main {
}
