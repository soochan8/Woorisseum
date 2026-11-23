# 우리씀 앱 아이콘

## 의미와 적용

두 사람의 기록이 한 권의 장부로 모이는 모습을 단순한 심볼로 표현했다. 청록 바탕·아이보리 장부·벽돌색 책갈피를 사용한다. 아이콘 안에 글자를 넣지 않아 작은 크기에서도 형태를 유지한다.

- 생성 원본: `woorisseum-app-icon-master.png` (1254×1254, 불투명 PNG)
- iOS: `../../iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` (1024×1024, 알파 채널 없음)
- Android: `../../androidApp/src/main/res/drawable-nodpi/woorisseum_launcher_art.png`와 해상도별 launcher PNG
- Android adaptive icon은 foreground에 8% inset을 적용한다. 원형 등 런처 마스크의 가장자리에서 심볼이 잘리는 것을 줄인다.
- iOS 모서리는 이미지에 굽지 않고 시스템 마스크에 맡긴다. iOS dark/tinted 전용 이미지는 별도로 지정하지 않았다.

## 제작 기록

내장 imagegen으로 새 이미지를 생성하고 한 차례 질감 제거를 요청했다. API/CLI 우회 방식은 사용하지 않았다. 배포용 크기 변환은 macOS `sips`로 수행했다.

생성 프롬프트:

> Use case: logo-brand. Asset type: final production mobile app icon, exactly one square image, no presentation sheet. Create an original restrained identity for Woorisseum / 우리씀, a Korean app where two partners keep a shared household ledger, budgets and grocery lists. Design a compact distinctive monogram-like symbol that feels like TWO simple softly rounded ivory paper ribbons joining into ONE open ledger / understated lowercase w. It should suggest togetherness and two parallel records without literal people, coins, hearts, shopping carts or a generic wallet. One tiny muted terracotta bookmark accent may be used, integral to the ledger, not a decorative dot. Background: completely flat opaque deep teal #245C52 edge-to-edge. Mark: warm ivory #FAF9F6, small terracotta #AE6552 accent. Premium everyday stationery, quiet confident graphic design, mathematically clean bold silhouettes, flat vector-like edges, no gradients, shadows, texture, 3D, glow or mockup. No lettering, text, numbers or watermark. Composition: exact centered mark contained within central 58 percent of both width and height, comfortable uniform negative space, strong legibility at 40 pixels. Full bleed 1:1 square with square corners; DO NOT draw an outer rounded rectangle or an icon border because the OS provides the mask. Deliver one finished icon only.

편집 프롬프트:

> Edit this exact app icon. Preserve the centered open-ledger / paired-record w-shaped ivory mark, the six horizontal cutouts, the small terracotta bookmark, scale and composition. Make it a completely flat, crisp production logo: REMOVE ALL grain, texture, lighting, vignette, shading, highlights and gradients. The entire background must be one exact solid color #245C52 including all four corners and all negative-space cutouts. Every ivory area must be one flat #FAF9F6, every bookmark area one flat #AE6552. Clean antialiased vector-like edges. Square full-bleed image, no border, no added words or shapes, opaque background.
