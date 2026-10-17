package pt.molduratv;

import android.app.AlertDialog;
import android.content.Context;
import java.util.*;

/** D-pad accessible, independent rules for photographs and videos. Changes commit only with Guardar. */
final class FilterDialog {
    private final Context context; private final AppSettings settings;
    FilterDialog(Context context,AppSettings settings) { this.context=context; this.settings=settings; }
    void show() {
        new AlertDialog.Builder(context).setTitle("Filtros de reprodução")
            .setItems(new String[]{"Fotografias","Vídeos","Ver seleção atual","Repor todos os conteúdos"},(d,which)-> {
                if(which<2) edit(which==0);
                else if(which==2) details();
                else new AlertDialog.Builder(context).setMessage("Reproduzir todos os tipos, orientações, proporções e formatos?")
                    .setPositiveButton("Repor",(dialog,button)-> { settings.filter=PlaybackFilter.all(); settings.save(); show(); }).setNegativeButton("Cancelar",null).show();
            }).setNegativeButton("Fechar",null).show();
    }
    private void edit(boolean photo) {
        PlaybackFilter.Rule rule=photo?settings.filter.photos:settings.filter.videos;
        new AlertDialog.Builder(context).setTitle(photo?"Filtros das fotografias":"Filtros dos vídeos")
            .setItems(new String[]{"Incluir na apresentação: "+(rule.enabled?"Sim":"Não"),"Orientações","Proporções","Formatos de ficheiro"},(d,which)-> {
                if(which==0) { settings.filter=settings.filter.with(photo,new PlaybackFilter.Rule(!rule.enabled,rule.orientations,rule.ratios,rule.formats,photo)); settings.save(); edit(photo); }
                else choose(photo,which);
            }).setNegativeButton("Voltar",(d,w)->show()).show();
    }
    private void choose(boolean photo,int dimension) {
        PlaybackFilter.Rule rule=photo?settings.filter.photos:settings.filter.videos;
        String[] keys=dimension==1?PlaybackFilter.ORIENTATIONS:dimension==2?PlaybackFilter.RATIOS:photo?PlaybackFilter.PHOTO_FORMATS:PlaybackFilter.VIDEO_FORMATS;
        String[] labels=dimension==1?PlaybackFilter.ORIENTATION_LABELS:dimension==2?PlaybackFilter.RATIO_LABELS:photo?PlaybackFilter.PHOTO_LABELS:PlaybackFilter.VIDEO_LABELS;
        Set<String> selected=new LinkedHashSet<>(dimension==1?rule.orientations:dimension==2?rule.ratios:rule.formats);
        boolean[] checks=new boolean[keys.length]; for(int i=0;i<keys.length;i++) checks[i]=selected.contains(keys[i]);
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle(dimension==1?"Orientações permitidas":dimension==2?"Proporções permitidas":"Formatos permitidos")
            .setMultiChoiceItems(labels,checks,(d,which,checked)-> { if(checked) selected.add(keys[which]); else selected.remove(keys[which]); })
            .setPositiveButton("Guardar",(d,w)-> {
                settings.filter=settings.filter.with(photo,new PlaybackFilter.Rule(rule.enabled,dimension==1?selected:rule.orientations,dimension==2?selected:rule.ratios,dimension==3?selected:rule.formats,photo));
                settings.save(); edit(photo);
            }).setNegativeButton("Cancelar",(d,w)->edit(photo)).setNeutralButton("Todos / Nenhum",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v-> {
            boolean all=selected.size()!=keys.length;
            selected.clear(); if(all) selected.addAll(Arrays.asList(keys)); for(int i=0;i<keys.length;i++) dialog.getListView().setItemChecked(i,all);
        })); dialog.show();
    }
    private void details() {
        String text="Dentro de cada lista, basta corresponder a uma seleção. Entre listas, o conteúdo tem de cumprir todas as condições.\nUma seleção vazia não permite conteúdos desse tipo.\nAs proporções consideram a orientação e admitem uma pequena tolerância.\n";
        text+=describe("Fotografias",settings.filter.photos,true)+describe("Vídeos",settings.filter.videos,false);
        new AlertDialog.Builder(context).setTitle("Seleção atual").setMessage(text).setPositiveButton("Voltar",(d,w)->show()).show();
    }
    private String describe(String title,PlaybackFilter.Rule rule,boolean photo) {
        return "\n"+title+": "+(rule.enabled?"incluídos":"excluídos")+"\nOrientações: "+labels(rule.orientations,PlaybackFilter.ORIENTATIONS,PlaybackFilter.ORIENTATION_LABELS)
            +"\nProporções: "+labels(rule.ratios,PlaybackFilter.RATIOS,PlaybackFilter.RATIO_LABELS)+"\nFormatos: "+labels(rule.formats,photo?PlaybackFilter.PHOTO_FORMATS:PlaybackFilter.VIDEO_FORMATS,photo?PlaybackFilter.PHOTO_LABELS:PlaybackFilter.VIDEO_LABELS)+"\n";
    }
    private String labels(Set<String> selected,String[] keys,String[] labels) { List<String> result=new ArrayList<>(); for(int i=0;i<keys.length;i++) if(selected.contains(keys[i])) result.add(labels[i]); return result.isEmpty()?"Nenhum":String.join(", ",result); }
}
