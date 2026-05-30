# راهنمای ساخت APK با GitHub Actions

## مراحل استفاده:

### 1. پوشه کردن پروژه در GitHub
ابتدا پروژه رو روی GitHub پوش کنید:

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
git push -u origin main
```

### 2. فعال سازی GitHub Actions
- به ریپازیتوری GitHub خودتون برید
- به تب Actions برید
- اگر پیامی ظاهر شد، اجازه بدید Actions فعال بشن

### 3. اجرای Workflow
Workflow به صورت خودکار در این مواقع اجرا می‌شه:
- وقتی کد جدیدی به برنچ main پوش می‌کنید
- وقتی Pull Request ایجاد می‌کنید
- یا می‌تونید به صورت دستی از تب Actions اجراش کنید

### 4. دانلود APK
- به تب Actions برید
- آخرین workflow رو انتخاب کنید
- در بخش Artifacts می‌تونید APK رو دانلود کنید

### فایل‌های خروجی:
- **app-debug.apk**: نسخه دیباگ برای تست
- **app-release.apk**: نسخه ریلیز (اگر signing کانفیگ شده باشه)

## نکات مهم:
- فایل‌های APK به مدت 7 روز در GitHub باقی می‌مونن
- برای ریلیز واقعی، باید signing کانفیگ کنید
- اگر نیاز به تغییر SDK یا تنظیمات خاص دارید، فایل `.github/workflows/build.yml` رو ویرایش کنید

## مشکلات احتمالی:
اگر build failed شد:
1. Check the Actions tab for error details
2. مطمئن شید که همه فایل‌های لازم وجود داره
3. اگر خطای مربوط به dependency هست، فایل `build.gradle.kts` رو چک کنید