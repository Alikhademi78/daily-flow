# راهنمای پروژه DailyFlow

## 🚀 ساخت APK با GitHub Actions (توصیه شده)

### مراحل ساخت APK بدون نیاز به نصب ابزارهای محلی:

#### 1. پوش کردن تغییرات به GitHub
```bash
# اضافه کردن تغییرات جدید
git add .
git commit -m "توضیح تغییرات"
git push origin main
```

#### 2. دریافت APK از GitHub Actions
- به repository برید: https://github.com/Alikhademi78/daily-flow
- تب **Actions** رو انتخاب کنید
- آخرین workflow رو انتخاب کنید
- در بخش **Artifacts** می‌تونید APK رو دانلود کنید

### فایل‌های خروجی:
- **app-debug.apk** - نسخه دیباگ برای تست
- **app-release.apk** - نسخه ریلیز (اگر signing کانفیگ شده باشه)

## 📋 دستورالعمل‌های سریع

### هر بار که تغییری دادید:
```bash
git add .
git commit -m "توضیح تغییر"
git push origin main
```

### چک کردن وضعیت build:
1. به GitHub برید
2. تب Actions رو باز کنید
3. صبر کنید workflow تموم بشه (5-10 دقیقه)
4. APK رو از بخش Artifacts دانلود کنید

## ⚠️ نکات مهم

- ✅ **فایل‌های حساس** مثل `.env` و `local.properties` در `.gitignore` هستن
- ✅ **فایل‌های اضافی محلی** حذف شدن (gradlew, etc)
- ✅ **GitHub Actions** به صورت خودکار کار می‌کنه
- ✅ **APK‌ها به مدت 7 روز** در GitHub باقی می‌مونن

## 🔄 اگر build failed شد:

1. به تب Actions برید
2. روی workflow failed کلیک کنید
3. خطاها رو چک کنید
4. اگر نیاز بود فایل `.github/workflows/build.yml` رو ویرایش کنید

## 📱 نصب APK روی گوشی:

1. APK رو دانلود کنید
2. در گوشی به Settings > Security برید
3. Unknown Sources رو فعال کنید
4. APK رو نصب کنید

---

**تاریخ آخرین بروزرسانی:** $(date +%Y-%m-%d)
**نسخه پروژه:** 1.0.0