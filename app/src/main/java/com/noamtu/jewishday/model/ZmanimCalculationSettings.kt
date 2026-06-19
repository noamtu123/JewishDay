package com.noamtu.jewishday.model

data class ZmanimCalculationSettings(
    val preset: ZmanimPreset = ZmanimPreset.Standard,
    val inIsrael: Boolean = true,
    val highLatitudeHandling: HighLatitudeHandling = HighLatitudeHandling.FixedMinutesFallback,
    val alotHashacharMethod: AlotHashacharMethod = AlotHashacharMethod.Degrees16Point1,
    val misheyakirMethod: MisheyakirMethod = MisheyakirMethod.Degrees11Point5,
    val sunriseMethod: SunriseMethod = SunriseMethod.SeaLevel,
    // Sof Zman Shema / Tefillah each show a GRA row and a Magen Avraham row. Each has its
    // own configurable method, and each picker only offers options from its own family.
    val sofZmanShemaGraMethod: SofZmanShemaMethod = SofZmanShemaMethod.Gra,
    val sofZmanShemaMethod: SofZmanShemaMethod = SofZmanShemaMethod.Mga72,
    val sofZmanTefillahGraMethod: SofZmanTefillahMethod = SofZmanTefillahMethod.Gra,
    val sofZmanTefillahMethod: SofZmanTefillahMethod = SofZmanTefillahMethod.Mga72,
    val chatzotMethod: ChatzotMethod = ChatzotMethod.Solar,
    val chatzotHaLailaMethod: ChatzotMethod = ChatzotMethod.Solar,
    val minchaGedolaMethod: MinchaGedolaMethod = MinchaGedolaMethod.Standard,
    val minchaKetanaMethod: MinchaKetanaMethod = MinchaKetanaMethod.Standard,
    val plagHaminchaMethod: PlagHaminchaMethod = PlagHaminchaMethod.Gra,
    val sunsetMethod: SunsetMethod = SunsetMethod.SeaLevel,
    val tzeitHakochavimMethod: TzeitHakochavimMethod = TzeitHakochavimMethod.Minutes20,
    val candleLightingMethod: CandleLightingMethod = CandleLightingMethod.Minutes18,
    val motzeiShabbatMethod: MotzeiShabbatMethod = MotzeiShabbatMethod.Geonim8Point5,
    val rabbeinuTamMethod: RabbeinuTamMethod = RabbeinuTamMethod.Minutes72,
    val bainHashmashotMethod: BainHashmashotMethod = BainHashmashotMethod.RabbeinuTam13Point24,
    val fastDayMethod: FastDayMethod = FastDayMethod.Alot72ToTzeit8Point5,
    val chametzMethod: ChametzMethod = ChametzMethod.Gra,
    val ateretTorahSunsetOffsetMinutes: Int = 40,
)

fun ZmanimPreset.defaultSettings(inIsrael: Boolean = true): ZmanimCalculationSettings = when (this) {
    ZmanimPreset.Standard -> ZmanimCalculationSettings(preset = this, inIsrael = inIsrael)
    ZmanimPreset.MagenAvraham72 -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = inIsrael,
        alotHashacharMethod = AlotHashacharMethod.Minutes72,
        sofZmanShemaMethod = SofZmanShemaMethod.Mga72,
        sofZmanTefillahMethod = SofZmanTefillahMethod.Mga72,
        minchaGedolaMethod = MinchaGedolaMethod.Mga72,
        minchaKetanaMethod = MinchaKetanaMethod.Mga72,
        plagHaminchaMethod = PlagHaminchaMethod.Mga72,
        tzeitHakochavimMethod = TzeitHakochavimMethod.Minutes72,
        rabbeinuTamMethod = RabbeinuTamMethod.Minutes72,
        chametzMethod = ChametzMethod.Mga72,
    )
    ZmanimPreset.MagenAvraham16Point1 -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = inIsrael,
        alotHashacharMethod = AlotHashacharMethod.Degrees16Point1,
        sofZmanShemaMethod = SofZmanShemaMethod.Mga16Point1,
        sofZmanTefillahMethod = SofZmanTefillahMethod.Mga16Point1,
        minchaGedolaMethod = MinchaGedolaMethod.Degrees16Point1,
        minchaKetanaMethod = MinchaKetanaMethod.Degrees16Point1,
        plagHaminchaMethod = PlagHaminchaMethod.Degrees16Point1,
        tzeitHakochavimMethod = TzeitHakochavimMethod.Degrees16Point1,
        chametzMethod = ChametzMethod.Mga16Point1,
    )
    ZmanimPreset.RabbeinuTam -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = inIsrael,
        tzeitHakochavimMethod = TzeitHakochavimMethod.Minutes72,
        motzeiShabbatMethod = MotzeiShabbatMethod.RabbeinuTam72,
        rabbeinuTamMethod = RabbeinuTamMethod.Minutes72,
        bainHashmashotMethod = BainHashmashotMethod.RabbeinuTam13Point24,
    )
    ZmanimPreset.Chabad -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = inIsrael,
        alotHashacharMethod = AlotHashacharMethod.BaalHatanya,
        sofZmanShemaMethod = SofZmanShemaMethod.BaalHatanya,
        sofZmanTefillahMethod = SofZmanTefillahMethod.BaalHatanya,
        minchaGedolaMethod = MinchaGedolaMethod.BaalHatanyaGreaterThan30,
        minchaKetanaMethod = MinchaKetanaMethod.BaalHatanya,
        plagHaminchaMethod = PlagHaminchaMethod.BaalHatanya,
        tzeitHakochavimMethod = TzeitHakochavimMethod.BaalHatanya,
        motzeiShabbatMethod = MotzeiShabbatMethod.BaalHatanya,
        fastDayMethod = FastDayMethod.BaalHatanya,
        chametzMethod = ChametzMethod.BaalHatanya,
    )
    ZmanimPreset.Sephardi -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = inIsrael,
        sofZmanShemaMethod = SofZmanShemaMethod.AteretTorah,
        sofZmanTefillahMethod = SofZmanTefillahMethod.AteretTorah,
        minchaGedolaMethod = MinchaGedolaMethod.AteretTorah,
        minchaKetanaMethod = MinchaKetanaMethod.AteretTorah,
        plagHaminchaMethod = PlagHaminchaMethod.AteretTorah,
        tzeitHakochavimMethod = TzeitHakochavimMethod.AteretTorah,
        motzeiShabbatMethod = MotzeiShabbatMethod.AteretTorah,
        candleLightingMethod = CandleLightingMethod.Minutes20,
    )
    ZmanimPreset.Ashkenazi -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = inIsrael,
        sofZmanShemaMethod = SofZmanShemaMethod.Mga72,
        sofZmanTefillahMethod = SofZmanTefillahMethod.Mga72,
        minchaGedolaMethod = MinchaGedolaMethod.Mga72,
        minchaKetanaMethod = MinchaKetanaMethod.Mga72,
        plagHaminchaMethod = PlagHaminchaMethod.Mga72,
        rabbeinuTamMethod = RabbeinuTamMethod.Minutes72,
        motzeiShabbatMethod = MotzeiShabbatMethod.RabbeinuTam72,
        chametzMethod = ChametzMethod.Mga72,
    )
    ZmanimPreset.Israeli -> ZmanimCalculationSettings(
        preset = this,
        inIsrael = true,
        chatzotMethod = ChatzotMethod.FixedLocal,
        sofZmanShemaMethod = SofZmanShemaMethod.FixedLocalGra,
        sofZmanTefillahMethod = SofZmanTefillahMethod.FixedLocalGra,
        minchaGedolaMethod = MinchaGedolaMethod.FixedLocal,
        minchaKetanaMethod = MinchaKetanaMethod.FixedLocal,
        plagHaminchaMethod = PlagHaminchaMethod.FixedLocal,
        candleLightingMethod = CandleLightingMethod.Minutes20,
    )
    ZmanimPreset.Custom -> ZmanimCalculationSettings(preset = this, inIsrael = inIsrael)
}
