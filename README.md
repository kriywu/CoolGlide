> 愿未来的自己看到这篇文章，庆幸自己的努力。 加油！

CoolGlide 是学习Glide的过程中，动手写的一个简单版本。主要是为了进一步了解Glide的设计原理。


## 背景

我在大三的时候写了一个简单的个人云盘，我想把手机里面的照片上传到云盘上，这之前需要展示手机里面的照片，最简单的做法就是像微信朋友圈发照片那样。但是实际上，整个网格列表特别的卡。卡的原因有两个：一是主线程中取加载Bitmap导致UI不能相应操作，二是内存中大量Bitmap，GC导致Stop the World。

后来我想出了一个方案：[尝试加载一千张照](https://blog.csdn.net/github_39994972/article/details/92785647)。 其实主要就是用自己在艺术探索中学习的知识，结合像线程池、LruCache等一定程度上把照片流畅的加载出来了。

但是代码写的都在Activity中，经过三个月实习，这样的代码实在太恶心了，很难移植，可读性太差。与此同时很多地方处理的很粗糙。但是那是一次勇敢的尝试，我也感谢那次尝试。

经过几个月实习，学习了很多编码和设计知识，越发觉得之前方案的恶心，我尝试取学习Glide，写了这个照片加载框架的Demo。可能在未来的某天回头看时候，这一份代码依然会让我感觉差劲，但是我依然会为自己的尝试而感到庆幸。



## 设计

还是这个场景，要实现发朋友圈时候哪个照片选择器（网格Recyclerview显示大量照片）。

整个框架的整体结构如下：**基于生产者消费者模型**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20190921133510102.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2dpdGh1Yl8zOTk5NDk3Mg==,size_16,color_FFFFFF,t_70)

1. 把每个加载照片任务抽象为一个**Class Request**。主要成员成员有照片的Uri，目标View。
2. Request任务有了，需要线程处理这些加载照片的耗时任务，继承Thread，写一个专门负责进行耗时操作的类RequestDispatcher。
3. 从Request 到 RequestDispatcher，中间缺少一个RequestManager管理Request请求队列。

## 详细

1. 在Activity中的使用，生成Request任务

   1. 获取CoolGlide单例
   2. 创建一个Request对象，要加载的照片Uri，需要attach的ImageView，设置加载成功回调
   3. 提交加载任务

   ```java
   // 1. 获取加载框架实例
   glide = RequestManager.getInstance();
   
   @Override
   public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
       ImageView iv = holder.itemView.findViewById(R.id.iv);
       // 2. 创建任务
       UriRequest request = new LocalRequest(list.get(position)); // url
       request.mTargetView = new SoftReference<>(iv); // view
       request.mListener = new LoadingListener() {
           @Override
           public void onSuccess(Bitmap bitmap) {
               Log.d(TAG, "onSuccess: success , position = " + position);
           }
   
           @Override
           public void onFail() {
               Log.d(TAG, "onFail: , position = " + position);
           }
       };
       // 3. 提交任务 
       glide.offer(request); // add Request Task to Request Queue
   }
   // 启动加载引擎
   glide.start();
   ```

2. RequestManger中将任务预处理，添加到消费者队列中

   1. 设置tag，因为Recyclerview的复用，要防止老的Bitmap加载到新的ViewHolder中
   2. 检查LruCache中是否命中缓存，如果命中直接显示
   3. 设置LoadingListener代理，目的是加载成功的时候将Bitmap添加到LruCache中
   4. 将任务添加到阻塞队列

   ```java
   public void offer(UriRequest request) {
       // 1. set tag, 防止RecyclerView中错位
       ImageView imageView = request.mTargetView.get();
       if(request.mTag != null
          && imageView.getTag() != null
          && request.mTag != imageView.getTag())
           imageView.setImageBitmap(null); // Recyclerview复用的时候，防止错位
       request.mTag = request.mUri;
       if (imageView != null) imageView.setTag(request.mTag);
       Log.d(TAG, "offer: set tag ok " + request.mUri);
   
       // 2. if cache has it， 命中LruCache中缓存，直接显示
       Bitmap bitmap;
       if ((bitmap = cache.get(request.mUri)) != null) {
           request.attach(bitmap);
           Log.d(TAG, "offer: hit cache " + request.mUri);
       }
   
       // 3. need to load it，这里代理LoadingListener，回调是将Bitmap添加到LruCache
       request.mListener = new LoadingListenerProxy(request.mListener) {
           @Override
           public void onSuccess(Bitmap bitmap) {
               cache.put(request.mUri, bitmap);
               super.onSuccess(bitmap);
           }
           @Override
           public void onFail() {
               super.onFail();
           }
       };
       // 4. add into request queue , which type is LinkedBlockingQueue
       requests.offer(request); // 添加到阻塞队列
       Log.d(TAG, "offer: loading" + request.mUri);
   }
   ```

3. RequestDispatcher 消费队列中的Request

   1. 检查当前任务是否被中断，结束线程
   2. 从任务队列中消费Request
   3. 设置占位图
   4. 真正加载Bitmap

   ```java
   @Override
   public void run() {
       while (true){
           // 1. check interrupt
           if(interrupted()) return;
           UriRequest request = null;
           // 2. take Request
           try {
               request = requests.take();
               // 3. set placeholder
               request.placeholder();
               // 4. load bitmap
               request.loadBitmap();  
           } catch (Exception e) {
               e.printStackTrace();
               if(request != null) request.mListener.onFail();
           }
       }
   }
   ```

4. Request 中提供加载照片的能力，因为不同的照片源加载（比如网络照片，本地照片）的逻辑不一样，这里为了提高扩展性。要加载特殊的照片只需要继承Request，并实现loadBitmap方法即可。

   1. 检查Soft引用的目标View是否被回收，如果回收了，结束加载任务
   2. 获取控件的width和height，计算采样率
   3. 将采样后的Bitmap加载到View上

   ```java
   public class LocalRequest extends UriRequest {
   	// ... 省略代码
       @Override
       public void loadBitmap() {
           // 1. check view
           View view = mTargetView.get();
           if (view == null) {
               Log.d(TAG, "loadBitmap: view == null");
               return;
           }
   
           //2. get w and h before decode bitmap
           SizeDeterminer sizeDeterminer = SizeDeterminer.Obtains(view, (width, height) -> {
               Log.d(TAG, "loadBitmap: width = " + width + " height = " + height);
               Bitmap fit = BitmapUtil.decodeBitmapFromFile(mUri, width, height);
               // 3. attach bitmap into view
               attach(fit);
           });
           sizeDeterminer.getSize();
       }
   }
   ```

5. 采样率的计算借用了Glide的思路

   1. 获取View的宽和高。可能是准确尺寸，也可能是Match_parent wrap_parent
   2. 如果是准确的w he h，回调到计算采样率
   3. 如果不是准确的w 和 h，需要获取View的ViewTreeObserver
   4. 给ViewTreeObserver添加PreDraw监听器，当View布局完成的时候会调用这个回调，布局完成意为着w和h已知。

   ```java
   public void getSize() {
       // 1. get view w and h params
       int currentWidth = getTargetWidth();
       int currentHeight = getTargetHeight();
   
       // 2. is valid  ( when w and h > 0) 
       if (isViewStateAndSizeValid(currentWidth, currentHeight)) {
           callback.onSizeReady(currentWidth, currentHeight);
           Log.d(TAG, "getSize: valid");
           return;
       }
   
       // 3. invalid ( w and h <= 0)
       ViewTreeObserver observer = mView.getViewTreeObserver();
       Log.d(TAG, "getSize: invalid");
   
       // 4. before drawing view, call SizeReady listener
       observer.addOnPreDrawListener(new SizeDeterminerLayoutListener(this));
   }
   ```

6. 测量目标View的尺寸

   1. onPreDraw被执行的时候完成了Layout，此时可以获取有效的 w 和 h
   2. 移除Listener，因为Draw会被执行很多次，onPreDraw也会被执行很多次
   3. onPreDraw是主线程，要切换到子线程，这里用一个线程池专门decode bitmap

   ```java
       private static final class SizeDeterminerLayoutListener implements ViewTreeObserver.OnPreDrawListener {
           private final WeakReference<SizeDeterminer> sizeDeterminerRef;
   		// 1. finished layout
           @Override
           public boolean onPreDraw() {
               SizeDeterminer sizeDeterminer = sizeDeterminerRef.get();
               if (sizeDeterminer != null) {
                   sizeDeterminer.checkCurrentDimens(this);
               }
               return true;
           }
       }
   	
       private void checkCurrentDimens(SizeDeterminerLayoutListener self) {
           int currentWidth = getTargetWidth();
           int currentHeight = getTargetHeight();
   
           if (!isViewStateAndSizeValid(currentWidth, currentHeight)) {
               Log.d(TAG, "checkCurrentDimens: size is invalid");
               return;
           }
           // 2. remove listener 
           ViewTreeObserver observer = mView.getViewTreeObserver();
           observer.removeOnPreDrawListener(self);
           free();
   		// 3. switch to thread from main thread
           RequestManager.getInstance().commit(() -> {
               callback.onSizeReady(currentWidth, currentHeight);
           });
       }
   }
   ```

7. onSizeReady被调用后，就可以按照w和h去计算采样率了。

   1. 获取采样率
   2. 按照采样率加载Bitmap

   ```java
   public static Bitmap decodeBitmapFromFile(String file, int width, int height) {
       // 1. get sample rate
       final BitmapFactory.Options options = new BitmapFactory.Options();
       options.inJustDecodeBounds = true;
       BitmapFactory.decodeFile(file, options);
   
       options.inSampleSize = calculateInSampleSize(options, width, height);
       options.inJustDecodeBounds = false;
       // 2. decode bitmap 
       return BitmapFactory.decodeFile(file, options);
   }
   
   private static int calculateInSampleSize(BitmapFactory.Options options, int width, int height) {
       final int outHeight = options.outHeight;
       final int outWidth = options.outWidth;
   
       if (outHeight < height || outWidth < width) {
           return 1;
       }
       return Math.min(outHeight / height, outWidth / width);
   }
   ```

8. 获取了Bitmap后将其加载到View上

   1. 检查Bitmap是否加载成功
   2. 检查View是否被回收
   3. 切换到主线程更新UI
   4. 缓存Bitmap

   ```java
   // in class Request
   public void attach(Bitmap bitmap) {
       // 1. check bitmap object 
       if(bitmap == null) {
           mListener.onFail();
           Log.d(TAG, "attach: bitmap == null");
           return;
       }
       // 2. check target view
       final ImageView imageView = mTargetView.get();
       if(imageView == null  || imageView.getTag() != mTag) {
           mListener.onFail();
           Log.d(TAG, "attach: view == null");
           return;
       }
       // 3. attach bitmap in main thread
       if(Looper.getMainLooper() == Looper.myLooper()){
           imageView.setImageBitmap(bitmap);
       }else{
           imageView.post(() -> imageView.setImageBitmap(bitmap));
       }
       // 4. cache bitmap
       mListener.onSuccess(bitmap);
   }
   ```



## 总结

这样一个流程就完成了图片加载框架。有以下特点

1. 继承Request 实现LoadBitmap扩张照片源。
2. 基于生产消费者模型，结构更清楚（相对于我之前写的）
3. 晚上的计算采样率计算方案，这部分采用Glide的做法
4. LruCache缓存策略
5. 目前代码还存在一个BUG，部分照片显示会缺失 =。=，找了好久还不知道为啥

