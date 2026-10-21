# xTrap

Paper 1.21.4+ için trap (arazi) eklentisi. Oyuncular chunk'lardan oluşan arazileri satın alır, üyelerini yönetir, can puanı biriktirir ve birbirleriyle savaşır.

Yapımcı: jamilaxd

## Ne işe yarar

- Arazi chunk'lardan kurulur. Bir oyuncu aynı anda birden fazla chunk'ı tek bir trapa bağlayabilir.
- Satın alınan arazide yabancı oyuncular blok kıramaz, koyamaz ve kapı, sandık gibi şeylere dokunamaz. Yetki rütbeye göre değişir.
- Her trapin canı vardır. Düşman kesince can artar veya azalır. Can sıfırlanırsa arazi çöker ve yağmaya açılır.
- Trap TNT'si arazi içinde patlayınca kırdığı blok kadar can düşürür. Bloklar bir süre sonra geri gelir.
- Lider başka bir trapa savaş açabilir. Savaş süresi ve puanları `config.yml` içinden ayarlanır.
- Satıştaki ve çökmüş arazilerin üzerinde hologram durur. FancyHolograms yüklüyse onu kullanır, yoksa Paper'ın yazı göstergesine düşer.
- Towny veya ClansPlus varsa aynı kasaba, ulus ya da klandaki oyuncular tek grup sayılır ve savaş o grubun bütün arazilerine yayılır.

## Kurulum

1. Sunucu Paper 1.21.4 veya daha yeni olmalı.
2. `xTrap-1.0.0.jar` dosyasını `plugins` klasörüne at.
3. İsteğe bağlı: Vault (ekonomi), PlaceholderAPI, Towny, ClansPlus, FancyHolograms.
4. Sunucuyu başlat. `plugins/xTrap` altında `config.yml`, `messages.yml` ve `traps.yml` oluşur.

Vault yoksa satın alma kapanır. `xtrap.admin.free` yetkisi olanlar yine ücretsiz alabilir.

## Arazi kurma

1. `/trapadmin wand` ile seçim çubuğunu al.
2. Eklemek istediğin chunk'taki bir bloğa sol veya sağ tıkla. Aynı chunk'a tekrar tıklayınca seçimden çıkar. Bakışın 120 blok menzile kadar geçerlidir (`settings.wand-reach`).
3. `/trapadmin create` ile araziyi kur. Fiyat vermezsen `trap-defaults.price` kullanılır ve arazi o fiyattan satışa çıkar. `/trapadmin create 75000` fiyatı elle belirler.
4. Seçim camları arazi kurulunca silinir.

Oyuncu o arazinin içindeyken `/trap al` der ve menüden satın alır.

## Oyuncu komutları

`/trap` komutunun kısa hali `/xtrap`.

| Komut | Ne yapar |
| --- | --- |
| `/trap` | Komut listesini gösterir |
| `/trap al` | Bulunduğun satıştaki araziyi satın alır |
| `/trap liste` | Satıştaki arazileri listeler |
| `/trap traplerim` | Sahip olduğun ve üye olduğun arazileri açar |
| `/trap info [id/isim]` | Arazi bilgisini gösterir |
| `/trap isimdegistir set <isim>` | Arazi adını değiştirir |
| `/trap isimdegistir reset` | Adı numarasina geri alır |
| `/trap davet <oyuncu>` | Oyuncuyu davet eder |
| `/trap davet kabul [isim]` | Daveti kabul eder |
| `/trap deny [isim]` | Daveti reddeder |
| `/trap leave [isim]` | Araziden ayrılır |
| `/trap yonet [numara]` | Yönetim menüsünü açar |
| `/trap sethome` | Bulunduğun yeri ev yapar |
| `/trap home [id/isim]` | Eve ışınlar |
| `/trap sat <fiyat>` | Araziyi satışa çıkarır |
| `/trap dusman <isim>` | Başka araziyi düşman yapar |
| `/trap dost <isim>` | Başka araziyi dost yapar |
| `/trap upgrade uye\|can\|depo` | Üye, can veya depo yükseltmesi alır |
| `/trap pv [sayfa]` | Depoyu açar |
| `/trap siege <id/isim>` | Savaş açar |
| `/trap reclaim` | Çökmüş araziyi kurtarır |
| `/trap kapat <id/isim>` | Araziyi bırakır |
| `/trap sifirla [id/isim]` | Araziyi ilk haline döndürür (lider) |

## Yönetici komutları

Hepsi `xtrap.admin` ister.

| Komut | Ne yapar |
| --- | --- |
| `/trapadmin wand` | Seçim çubuğunu verir |
| `/trapadmin create [fiyat]` | Seçilen chunk'lardan arazi kurar ve satışa çıkarır |
| `/trapadmin delete <id>` | Araziyi siler |
| `/trapadmin setprice <id> <fiyat>` | Satış fiyatını değiştirir |
| `/trapadmin sethealth <id> <miktar>` | Canı ayarlar |
| `/trapadmin setowner <id> <oyuncu>` | Sahibi değiştirir |
| `/trapadmin info <id>` | Arazi bilgisini yazar |
| `/trapadmin tp <id>` | Araziye ışınlar |
| `/trapadmin tnt [adet]` | Trap TNT'si verir |
| `/trapadmin reset <id>` | Araziyi kayıtlı ilk haline döndürür |
| `/trapadmin recapture <id>` | Şu anki hali ilk hali olarak kaydeder |
| `/trapadmin list` | Bütün arazileri listeler |
| `/trapadmin reload` | Ayarları ve mesajları yeniden yükler |
| `/trapadmin save` | Arazileri diske yazar |

## Yetkiler

| Yetki | Varsayılan | Ne işe yarar |
| --- | --- | --- |
| `xtrap.use` | herkes | Temel komutlar |
| `xtrap.admin` | op | Yönetici komutları |
| `xtrap.admin.bypass` | op | Arazi korumasını atlar |
| `xtrap.admin.free` | op | Ücretsiz satın alır |
| `xtrap.siege` | herkes | Savaş açabilir |
| `xtrap.tnt` | herkes | Trap TNT'si kullanabilir |
| `xtrap.tnt.fast` | kimse | Fitil süresini kısaltır |

## PlaceholderAPI

Bulunduğun arazinin değerleri. Arazi yoksa boş döner.

`%trapcore_name%` `%trapcore_id%` `%trapcore_owner%` `%trapcore_health%` `%trapcore_max_health%` `%trapcore_members%` `%trapcore_member_list%` `%trapcore_online%` `%trapcore_kills%` `%trapcore_deaths%` `%trapcore_price%` `%trapcore_sale_price%` `%trapcore_default_price%` `%trapcore_status%` `%trapcore_status_raw%` `%trapcore_forsale%` `%trapcore_collapsed%` `%trapcore_reclaim_left%`

## Ayar dosyaları

- `config.yml` fiyat, can, savaş, TNT, koruma ve hologram ayarları.
- `messages.yml` bütün oyuncu mesajları. Renkler `&` ile yazılır.
- `traps.yml` kurulan arazilerin kaydı. Elle düzenleme sunucu kapalıyken yapılmalıdır.

`/trapadmin reload` ayarları ve mesajları yeniden okur. Arazi kayıtlarını yeniden yüklemez.

## Derleme

Java 21 gerekir.

```
gradlew jar
```

Çıktı `build/libs/xTrap-1.0.0.jar` olur. FancyHolograms derleme sırasında `libs/FancyHolograms-2.4.2.jar` üzerinden okunur, jar'ın içine girmez. Sunucuda eklenti olarak ayrıca yüklü olmalıdır.
