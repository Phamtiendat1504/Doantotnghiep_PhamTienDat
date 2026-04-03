# AGENTS.md - AI Coding Assistant Guidelines

## Architecture Overview
This is a Kotlin Android app using MVVM architecture with Firebase backend for room rental listings. Key components:
- **Models** (`Model/`): Data classes like `Room.kt` (comprehensive rental properties) and `User.kt` (tenant/landlord roles)
- **ViewModels** (`ViewModel/`): Business logic with LiveData, e.g., `PostViewModel.kt` handles room posting with validation
- **Views** (`View/`): Fragments/Activities using ViewBinding, adapters for RecyclerViews (horizontal/vertical layouts)
- **Repository** (`repository/`): Firebase operations, e.g., `RoomRepository.kt` manages Firestore/Storage with custom bucket `gs://doantotnghiep-b39ae.firebasestorage.app`

## Critical Workflows
- **Build**: Use `./gradlew build` or Android Studio; debug with `adb logcat`
- **Test**: Unit tests in `test/`, instrumentation in `androidTest/` (JUnit/Espresso)
- **Firebase**: Auth via email/phone, Firestore for data, Storage for images; rooms have `status: "pending"|"approved"|"rejected"`

## Project Conventions
- **Localization**: Vietnamese UI text/comments; use `NumberFormatUtils.kt` for currency formatting (e.g., `1.000.000 đ`)
- **Addresses**: Hardcoded Hanoi wards/communes in `AddressData.kt`; parse as `"Ward (District)"`
- **Images**: Glide for loading; max 10 photos per room; upload to `rooms/{roomId}/` with UUID filenames
- **Validation**: Client-side checks in ViewModels (e.g., required fields, phone length); server-side via Firebase rules
- **Naming**: PascalCase for classes, camelCase for methods; Firebase collections: `rooms`, `users`

## Integration Points
- **Firebase Auth**: Register/login with email; OTP for password reset via `sendPasswordResetEmail`
- **Firestore**: Query approved rooms by `status`, `isFeatured`; real-time listeners not used
- **Storage**: Custom bucket; delete old images on update (see `EditPostActivity.kt`)
- **External**: No APIs; Glide for image display with placeholders

## Key Files for Reference
- `app/build.gradle.kts`: Dependencies (Firebase BOM, Glide, ViewBinding enabled)
- `MainActivity.kt`: BottomNavigation with Fragments (Home/Search/Post/Profile)
- `HomeFragment.kt`: Loads featured/new rooms via Firestore queries
- `RoomAdapter.kt`: Dual view types with Vietnamese currency formatting</content>
<parameter name="filePath">C:\Users\tiend\AndroidStudioProjects\Doantotnghiep\AGENTS.md
