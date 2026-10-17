package pt.molduratv;

import android.app.AlertDialog;
import android.content.*;
import java.text.DateFormat;
import java.util.*;

final class Diagnostics {
    private final SharedPreferences preferences;
    private final FailureLog log;
    Diagnostics(Context context) {
        preferences = context.getSharedPreferences("diagnostics",0);
        log = FailureLog.decode(preferences.getString("history","[]"));
    }
    void record(String file,String category,String detail) {
        log.add(file,category,detail,System.currentTimeMillis()); save();
    }
    void playback(String file,androidx.media3.common.PlaybackException error) {
        int code=error.errorCode;
        String category,detail;
        if(code>=2000 && code<3000) { category="Leitura e acesso"; detail="Não foi possível ler o ficheiro. Verifica a ligação e as permissões."; }
        else if(code>=3000 && code<4000) { category="Formato do ficheiro"; detail="O leitor não conseguiu interpretar este ficheiro."; }
        else if(code>=4000 && code<5000) { category="Descodificação"; detail="O descodificador disponível não conseguiu reproduzir este conteúdo."; }
        else if(code>=5000 && code<6000) { category="Saída de áudio"; detail="Não foi possível inicializar ou manter o som."; }
        else { category="Reprodução"; detail="O leitor interrompeu a reprodução deste ficheiro."; }
        record(file,category,detail+" ("+error.getErrorCodeName()+")");
    }
    private void save() {
        try { preferences.edit().putString("history",log.encode()).apply(); } catch (org.json.JSONException ignored) { }
    }
    void show(Context context,Runnable rebuild) {
        StringBuilder body = new StringBuilder();
        for (FailureLog.Entry entry : log.entries()) body.append(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(entry.time)))
            .append(" · ").append(entry.category).append("\n").append(entry.file).append("\n")
            .append(entry.detail).append(entry.count > 1 ? " ("+entry.count+" vezes)" : "").append("\n\n");
        new AlertDialog.Builder(context).setTitle("Diagnóstico · últimas ocorrências")
            .setMessage(body.length() == 0 ? "Ainda não há ocorrências registadas." : body.toString())
            .setPositiveButton("Fechar",null).setNeutralButton("Limpar histórico",(d,w) -> { log.clear(); save(); })
            .setNegativeButton("Reconstruir índice",(d,w) -> rebuild.run()).show();
    }
}
