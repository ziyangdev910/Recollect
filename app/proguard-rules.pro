# 应用级 R8 规则（release 构建）。
# Room、Compose、ML Kit、Kotlin 等依赖自带的 consumer rules 已覆盖各自反射/入口；
# 这里只补充没有自带规则、且通过 JNI 绑定的库。

# OpenCV：Java 方法与 native 层通过 JNI 名称绑定，裁剪或重命名会导致 UnsatisfiedLinkError。
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ML Kit 的可选依赖在端侧不存在时会出现在缺失类检查中。
-dontwarn com.google.mlkit.**
