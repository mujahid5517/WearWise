# WearWise — User Photo Virtual Try-On, Expanded Closet & API Key Management

WearWise will be upgraded to put the user at the absolute center of the virtual try-on experience by supporting their own full-body/portrait photo as the primary try-on canvas, expanding every closet category with relaxed weekend streetwear and athletic luxury pieces for outfit comparison, and adding a custom Gemini API Key manager with live connection testing in the Profile screen.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and user experience choices have been aligned based on your preferences:

- **Confirmed Decision 1 (User Photo Avatar Canvas)**:
  - Users can capture or upload their photo directly in their **Profile** to serve as their permanent personal try-on avatar.
  - The **AI Virtual Try-On Studio** automatically loads the user's real photo as the primary canvas, sending it to Gemini's multimodal synthesis model (`gemini-2.5-flash-image`) to realistically drape selected garments directly over the user's actual body and posture.
  - On-the-fly camera snap or gallery import remains instantly accessible inside the Try-On Studio.

- **Confirmed Decision 2 (Expanded Closet with Relaxed Streetwear & Athletic Luxury)**:
  - Every closet category (**Tops, Bottoms, Outerwear, Shoes, Knitwear, Accessories**) is expanded with high-end relaxed streetwear and athletic luxury pieces.
  - Users can swap and compare diverse silhouettes (e.g., Structured Academic Tailor vs. Relaxed Tech Streetwear) both on the Home screen and in the Try-On Layer Stack.

- **Confirmed Decision 3 (Custom Gemini API Key Manager in Profile)**:
  - A dedicated **Gemini AI Settings** card in the Profile screen features a secure API dialog with:
    1. Obscured key input with show/hide toggle.
    2. **Test Connection** button that makes a quick verification ping to the Gemini API with immediate success/error feedback.
    3. **Save** & **Clear / Reset** actions.
  - Persisted in local encrypted preferences, prioritizing the user's custom key across all try-on and recommendation features while cleanly falling back to build configuration if none is set.

---

## 1. Overview & Core Concept

### What It Does
- **Personalized Try-On**: Transforms the AI Virtual Try-On Studio from an editorial model preview to an intimate personal fitting room where outfits are synthesized directly onto the user's real photo.
- **Side-by-Side Silhouette Comparison**: Doubles the wardrobe variety with modern athletic luxury and relaxed streetwear garments across all 6 categories, allowing multi-look comparison.
- **User API Autonomy**: Empowers users to connect their personal Google Gemini API keys directly within the app, inspect connection latency, and generate unlimited high-resolution virtual try-on renders.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. Profile: Set Personal Photo & Gemini API Key                             │
│    - Profile Screen -> Tap "Avatar Photo" -> Camera / Gallery -> Saved      │
│    - Tap "Configure Gemini API" -> Enter Key -> Tap "Test Connection"       │
│    - Verified green badge -> "Save API Key"                                 │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. Digital Wardrobe & Garment Comparison                                    │
│    - Closet Screen: View Academic Capsule + Relaxed Streetwear Items        │
│    - Compare items in Tops, Bottoms, Outerwear, Shoes, Knitwear, Accents    │
│    - Home Screen: Tap Swap icon on any layer to choose new streetwear pieces│
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. Virtual Try-On Studio with User's Real Photo                             │
│    - Try-On Studio opens with user's actual photo loaded automatically       │
│    - Swap layers (e.g. Technical Bomber + Wide Cargo Pants + Court Low-Tops)│
│    - Tap "SIMULATE VIRTUAL TRY-ON" -> Gemini synthesizes outfit onto user   │
│    - Real-time drape & fit gauge evaluated with personalized harmony score  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Obsidian & Midnight Violet Foundation**: `#0B0D19` dark canvas with `#1B2032` elevated cards and `#7C3AED` royal violet highlights.
- **Couture Amber Accents**: `#F59E0B` for connection status, harmony scores, and interactive swap indicators.
- **Profile API Card**: Clean elevated card with a lock/key badge, status pill ("Custom Key Active" vs "Default Key Active"), and a "Test Key" action with animated feedback.

---

## 3. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                               Presentation                                  │
│   ┌─────────────────────┐  ┌─────────────────────┐  ┌────────────────────┐  │
│   │    ProfileScreen    │  │     HomeScreen      │  │    TryOnScreen     │  │
│   │ - Avatar Photo Pick │  │ - Garment Swap      │  │ - User Photo Canvas│  │
│   │ - API Config Dialog │  │ - Multi-Fit Compare │  │ - Gemini Multimodal│  │
│   └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬──────────┘  │
└──────────────┼────────────────────────┼───────────────────────┼─────────────┘
               │                        │                       │
               ▼                        ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 Domain                                      │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │ UserProfileUseCase • ManageWardrobeUseCase • ExecuteTryOnUseCase      │  │
│   │ - getUserApiKey() / saveUserApiKey() / testApiKey()                  │  │
│   │ - getExpandedWardrobeItems()                                         │  │
│   │ - simulateTryOn(userPhotoUri, customApiKey, garments)                │  │
│   └──────────────────────────────────┬───────────────────────────────────┘  │
└──────────────────────────────────────┼──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  Data                                       │
│   ┌────────────────────────┐  ┌──────────────────┐  ┌────────────────────┐  │
│   │   ApiKeyManager (Pref) │  │  Room Database   │  │   GeminiApiService │  │
│   │   Encrypted storage    │  │  - UserProfile   │  │   - 2.5-flash-image│  │
│   │   for user Gemini key  │  │  - ClothingItems │  │   - 3.5-flash ping │  │
│   │                        │  │  - TryOnPreviews │  │                    │  │
│   └────────────────────────┘  └──────────────────┘  └────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Specific Additions

1. **New Wardrobe Garments (DatabaseSeeder.kt)**:
   - **Tops**: "Oversized Heavyweight Cotton Tee" (Matte Noir, 280 GSM luxury jersey)
   - **Bottoms**: "Pleated Wide-Leg Cargo Trousers" (Olive Drab, technical washed twill)
   - **Outerwear**: "Structured Technical Bomber" (Matte Charcoal, storm-resistant nylon)
   - **Shoes**: "Minimalist Leather Court Low-Tops" (Triple White, Italian nappa)
   - **Knitwear**: "Relaxed Waffle-Knit Mockneck" (Sand Beige, 100% organic cotton)
   - **Accessories**: "Matte Black Crossbody Sling Bag" (Technical Cordura, gunmetal buckles)

2. **User Photo & API Key Storage**:
   - `ApiKeyManager`: Local persistent key-value store using encrypted preferences for user API keys.
   - `UserProfile`: `profileImage` stores the user's permanent photo URI, automatically propagated to `TryOnViewModel`.

3. **Try-On Multimodal Synthesis**:
   - When user photo is present, base64 payload is attached as `inlineData` alongside prompt:
     `"High-fashion virtual try-on portrait of THIS SPECIFIC PERSON from the provided reference photo wearing: [Garment Descriptions]. Preserve their facial features, body proportions, skin tone, and hair while photorealistically draping the specified garments."`
   - When Gemini returns synthesized image, saves to local cache and displays in `AsyncImage`.

---

## 4. Verification Plan

1. **Compilation**: Run `compile_applet` to verify zero compile or import errors.
2. **API Key Dialog**: Verify opening dialog, entering key, tapping "Test Connection", saving, and clearing key.
3. **Closet Comparison**: Verify all 6 categories have multiple items in Closet screen and in Garment Swap dialog.
4. **User Photo Try-On**: Verify setting user photo in Profile and Try-On Studio, and running simulation with instant visual feedback.
