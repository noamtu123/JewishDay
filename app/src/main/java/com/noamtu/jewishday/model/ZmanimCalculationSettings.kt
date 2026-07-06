package com.noamtu.jewishday.model

data class ZmanimCalculationSettings(
    val preset: ZmanimPreset = ZmanimPreset.Standard,
    val inIsrael: Boolean = true,
    val useElevation: Boolean = false,
    val alotHashacharMethod: AlotHashacharMethod = AlotHashacharMethod.Degrees16Point1,
    val misheyakirMethod: MisheyakirMethod = MisheyakirMethod.Degrees11,
    val sunriseMethod: SunriseMethod = SunriseMethod.SeaLevel,
    // Sof Zman Shema / Tefillah each show a GRA row and a Magen Avraham row. Each has its
    // own configurable method, and each picker only offers options from its own family.
    val sofZmanShemaGraMethod: SofZmanShemaMethod = SofZmanShemaMethod.Gra,
    val sofZmanShemaMethod: SofZmanShemaMethod = SofZmanShemaMethod.Mga16Point1,
    val sofZmanTefillahGraMethod: SofZmanTefillahMethod = SofZmanTefillahMethod.Gra,
    val sofZmanTefillahMethod: SofZmanTefillahMethod = SofZmanTefillahMethod.Mga72,
    val chatzotMethod: ChatzotMethod = ChatzotMethod.Solar,
    val chatzotHaLailaMethod: ChatzotMethod = ChatzotMethod.Solar,
    val minchaGedolaMethod: MinchaGedolaMethod = MinchaGedolaMethod.Standard,
    val minchaKetanaMethod: MinchaKetanaMethod = MinchaKetanaMethod.Standard,
    val plagHaminchaMethod: PlagHaminchaMethod = PlagHaminchaMethod.Gra,
    val sunsetMethod: SunsetMethod = SunsetMethod.SeaLevel,
    val tzeitHakochavimMethod: TzeitHakochavimMethod = TzeitHakochavimMethod.Degrees6Point2,
    val candleLightingMethod: CandleLightingMethod = CandleLightingMethod.Minutes18,
    val motzeiShabbatMethod: MotzeiShabbatMethod = MotzeiShabbatMethod.Geonim8Point5,
    val rabbeinuTamMethod: RabbeinuTamMethod = RabbeinuTamMethod.Minutes72,
    val chametzMethod: ChametzMethod = ChametzMethod.Gra,
    val ateretTorahSunsetOffsetMinutes: Int = 40,
)
