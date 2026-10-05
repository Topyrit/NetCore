package netcore;

import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.maps.Map;

public class NetCoreUploadUi {
    private static String selectedMode = "Выживание";

    public static void showUploadMenu() {
        Dialog dialog = new Dialog("Загрузка карты");
        Table t = dialog.cont;
        t.defaults().pad(10f).width(400f);

        t.add("[lightgray]Выбери режим, для которого предназначена карта:[]").center().row();
        
        Table modesTable = new Table();
        String[] modes = {"Выживание", "Атака", "ПВП", "Песочница", "Аркада"};
        
        for (String mode : modes) {
            modesTable.button(mode, () -> {
                selectedMode = mode;
                Vars.ui.showInfoToast("Выбран режим: " + mode, 1f);
            }).size(120f, 40f).pad(2f);
            if (modesTable.getChildren().size % 3 == 0) modesTable.row();
        }
        t.add(modesTable).row();

        t.button("Выбрать карту из галереи", Icon.file, () -> {
            Dialog mapSelect = new Dialog("Выберите карту");
            Table mapList = new Table();
            mapSelect.cont.pane(mapList).size(500f, 400f);

            for (Map map : Vars.maps.customMaps()) {
                mapList.button(map.name(), () -> {
                    mapSelect.hide();
                    dialog.hide();
                    
                    String mapName = map.name();
                    String mapAuthor = map.author().isEmpty() ? "Неизвестный автор" : map.author();
                    String mapDesc = map.description().isEmpty() ? "Описание отсутствует." : map.description();

                    NetCoreUploader.uploadMap(map.file, mapName, mapAuthor, selectedMode, mapDesc, () -> {});
                }).width(460f).pad(5f).row();
            }

            mapSelect.addCloseButton();
            mapSelect.show();
        }).size(300f, 50f).padTop(20f);

        dialog.addCloseButton();
        dialog.show();
    }
                 }
