package pt.molduratv;

import android.app.*;
import android.content.*;
import android.provider.Settings;

final class ScreensaverSettings {
    static void show(Activity activity) {
        new AlertDialog.Builder(activity).setTitle("Protetor de ecrã MolduraTV")
            .setMessage("O protetor está incluído e disponível por predefinição. Usa as pastas, filtros, ordem, intervalos, divisão do ecrã e som da apresentação.\n\nNas definições do Android, escolhe MolduraTV como protetor de ecrã e define quando deve iniciar. O Android exige que esta escolha seja feita pelo utilizador; algumas boxes não disponibilizam essa opção.\n\nAo usar o comando, o protetor termina e regressa ao sistema.")
            .setPositiveButton("Abrir definições",(d,w)-> {
                try { activity.startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
                catch(ActivityNotFoundException | SecurityException unavailable) {
                    new AlertDialog.Builder(activity).setMessage("Esta box não disponibiliza o ecrã padrão de protetores. Procura Protetor de ecrã / Modo ambiente nas definições do equipamento.").setPositiveButton("OK",null).show();
                }
            }).setNegativeButton("Fechar",null).show();
    }
}
