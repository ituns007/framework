package org.ituns.framework.master.tools.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;

import org.ituns.framework.master.service.logcat.Logcat;
import org.ituns.framework.master.tools.network.IUri;
import org.ituns.framework.master.tools.storage.IStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class IBitmap {
    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;
    public static final int GB = 1024 * 1024 * 1024;

    public static Reader read(Image image) {
        return read(IImage.jpeg(image));
    }

    public static Reader read(Context context, Uri uri) {
        return read(IStream.input(IUri.with(uri).input(context)).readBytes());
    }

    public static Reader read(byte[] bytes) {
        return new Reader(bytes);
    }

    public static Transform transform(Bitmap bitmap) {
        return new Transform(bitmap);
    }

    public static Compress compress(Bitmap bitmap) {
        return new Compress(bitmap);
    }

    public static Writer write(Bitmap bitmap) {
        return new Writer(bitmap);
    }

    public static class Reader {
        private final byte[] data;
        private final BitmapFactory.Options options;

        private Reader(byte[] data) {
            this.data = data;
            this.options = decodeOptions(data);
        }

        private BitmapFactory.Options decodeOptions(byte[] bytes) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                return options;
            } catch (Exception e) {
                Logcat.e(e);
                return null;
            }
        }

        public Reader format(Bitmap.Config format) {
            if(options != null) {
                options.inPreferredConfig = format;
            }
            return this;
        }

        public Reader maxWidth(int maxWidth) {
            if(options != null && maxWidth > 0) {
                int outWidth = options.outWidth;
                if(outWidth > 0) {
                    int sampleSize = Math.max(1, options.inSampleSize);
                    while ((outWidth / sampleSize) > maxWidth) {
                        sampleSize *= 2;
                    }
                    options.inSampleSize = sampleSize;
                }
            }
            return this;
        }

        public Reader maxHeight(int maxHeight) {
            if(options != null && maxHeight > 0) {
                int outHeight = options.outHeight;
                if(outHeight > 0) {
                    int sampleSize = Math.max(1, options.inSampleSize);
                    while ((outHeight / sampleSize) > maxHeight) {
                        sampleSize *= 2;
                    }
                    options.inSampleSize = sampleSize;
                }
            }
            return this;
        }

        public Reader memLength(int memLength) {
            if(options != null && 0 < memLength) {
                int outWidth = options.outWidth;
                int outHeight = options.outHeight;
                if(outWidth >0 && outHeight > 0) {
                    int pixelBytes = pixelBytes(options.inPreferredConfig);
                    int sampleSize = Math.max(1, options.inSampleSize);
                    while ((outWidth / sampleSize) * (outHeight / sampleSize) * pixelBytes > memLength) {
                        sampleSize *= 2;
                    }
                    options.inSampleSize = sampleSize;
                }
            }
            return this;
        }

        private int pixelBytes(Bitmap.Config config) {
            switch (config) {
                case RGB_565:
                case ARGB_4444:
                    return 2;
                case ARGB_8888:
                    return 4;
                case RGBA_F16:
                    return 8;
                default:
                    return 1;
            }
        }

        public Reader reset() {
            if(options != null) {
                options.inSampleSize = 1;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            }
            return this;
        }

        public Bitmap bitmap() {
            if(data == null) return null;
            if(options != null) {
                options.inJustDecodeBounds = false;
            }
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }

        public Transform transform() {
            return new Transform(bitmap());
        }

        public Compress compress() {
            return new Compress(bitmap());
        }

        public Writer write() {
            return new Writer(bitmap());
        }
    }

    public static class Transform {
        private final Bitmap bitmap;
        private final Matrix matrix;

        private Transform(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.matrix = new Matrix();
        }

        public Transform reset() {
            this.matrix.reset();
            return this;
        }

        public Transform skew(float kx, float ky) {
            matrix.postSkew(kx, ky);
            return this;
        }

        public Transform skew(float kx, float ky, float px, float py) {
            matrix.postSkew(kx, ky, px, py);
            return this;
        }

        public Transform scale(float sx, float sy) {
            this.matrix.postScale(sx, sy);
            return this;
        }

        public Transform scale(float sx, float sy, float px, float py) {
            this.matrix.postScale(sx, sy, px, py);
            return this;
        }

        public Transform rotate(float degrees) {
            this.matrix.postRotate(degrees);
            return this;
        }

        public Transform rotate(float degrees, float px, float py) {
            this.matrix.postRotate(degrees, px, py);
            return this;
        }

        public Transform translate(float dx, float dy) {
            this.matrix.postTranslate(dx, dy);
            return this;
        }

        public Bitmap bitmap() {
            try {
                Bitmap result = Bitmap.createBitmap(bitmap,
                        0, 0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix, false);
                if(!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return result;
            } catch (Exception e) {
                Logcat.e(e);
                return null;
            }
        }

        public Compress compress() {
            return new Compress(bitmap());
        }

        public Writer write() {
            return new Writer(bitmap());
        }
    }

    public static class Compress {
        private final Bitmap bitmap;
        private int quality;
        private int maxBytes;
        private Bitmap.CompressFormat format;

        private Compress(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.quality = 100;
            this.maxBytes = Integer.MAX_VALUE;
            this.format = Bitmap.CompressFormat.JPEG;
        }

        public Compress quality(int quality) {
            if(0 <= quality && quality <=100) {
                this.quality = quality;
            }
            return this;
        }

        public Compress maxBytes(int maxBytes) {
            if(maxBytes > 0) {
                this.maxBytes = maxBytes;
            }
            return this;
        }

        public Compress format(Bitmap.CompressFormat format) {
            this.format = format;
            return this;
        }

        public void compressTo(OutputStream os) {
            try {
                bitmap.compress(format, calculateQuality(bitmap, quality), os);
                if(!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            } catch (Throwable e) {
                Logcat.e(e);
            }
        }

        private int calculateQuality(Bitmap bitmap, int quality) {
            //Bitmap ???????????????
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            //???????????????????????????
            float maxLength = maxBytes;

            //?????????????????????????????????????????????????????????????????????????????????????????????
            float qualityLength = calculateLength(bitmap, quality, stream);
            if(qualityLength <= maxLength) {
                return quality;
            }

            //???????????????0??????????????????quality
            int min = 0, max = quality;
            while (max - min > 3){
                int ave = (min + max) / 2;

                //???????????????????????????????????????
                qualityLength = calculateLength(bitmap, ave, stream);

                //???????????????????????????????????????????????????
                float percent = qualityLength / maxLength;

                if(0.985f <= percent && percent <= 1.0f) {  //?????????98.5% ~ 100%?????????????????????????????????
                    return ave;
                } else if(percent < 0.985f) {               //????????????98.5%?????????????????????
                    min = ave;
                } else {                                    //????????????100%?????????????????????
                    max = ave;
                }
            }
            return min; //????????????????????????
        }

        private float calculateLength(Bitmap bitmap, int quality, ByteArrayOutputStream stream) {
            try {
                stream.reset();
                bitmap.compress(format, quality, stream);
                return stream.size();
            } catch (Throwable e) {
                Logcat.e(e);
                return 0;
            }
        }

        public byte[] bytes() {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            compressTo(os);
            return os.toByteArray();
        }

        public Writer write() {
            return new Writer(bytes());
        }
    }

    public static class Writer {
        private final byte[] bytes;

        private Writer(byte[] bytes) {
            this.bytes = bytes;
        }

        private Writer(Bitmap bitmap) {
            this(bitmapToBytes(bitmap));
        }

        private static byte[] bitmapToBytes(Bitmap bitmap) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                if(!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return stream.toByteArray();
            } catch (Exception e) {
                Logcat.e(e);
                return new byte[]{};
            }
        }

        public byte[] bytes() {
            return bytes;
        }

        public boolean writeTo(Context context, Uri uri) {
            return writeTo(IUri.with(uri).output(context));
        }

        public boolean writeTo(OutputStream os) {
            try {
                os.write(bytes);
                os.flush();
                os.close();
                return true;
            } catch (Exception e) {
                Logcat.e(e);
                return false;
            }
        }
    }
}
