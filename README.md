## CoolGlide introduce
This is an image loading framework. It's prototype is Glide !


## dir structure

**core**

clz Request : 加载照片任务的封装类，网络Request， 文件Request ...

clz RequestManager ： 管理整个任务队列，对外提供API

clz ReqeustDispatcher : 继承自Thread， 负责耗时的加载任务

clz SizeDeterminer ： 负责获取View的真实大小

inf LoadingListener : 当照片加载完成的时候，回调

inf SizeReadCallBack ： 当View尺寸获取的时候，回调

**other**

clz MainActivity：测试的列表

clz LocalPhotoActivity ： 测试加载本地照片

clz WebPhotoActivity : 测试加载网络照片

clz BitmapUtil ： 一些通用的处理Bitmap的方法


# Usage
```java
// init Activity#onCreate
requestManager.start(); // 初始化线程和线程池

// add task
UriRequest request = new LocalRequest(uri); // url
request.mTargetView = new SoftReference<>(iv);
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
requestManager.offer(request);

// close Activity#onDestory()
requestManager.stop();
```
