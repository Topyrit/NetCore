package netcore;

import arc.Core;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.util.Http;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import arc.util.serialization.JsonValue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

public class NetCoreMod extends Mod {

    @Override
    public void init() {
        Vars.ui.menufrag.addButton("NetCore", Icon.logic, () -> {
            showNetCoreMenu();
        });
    }

    private void showNetCoreMenu() {
        Dialog dialog = new Dialog("NetCore Browser");
        dialog.cont.clear();
        dialog.cont.defaults().size(300f, 60f).pad(10f);
        
        dialog.cont.button("Загрузить карту", Icon.upload, () -> {
            NetCoreUploadUi.showUploadMenu();
        });
        
        dialog.cont.row();
        
        dialog.cont.button("Выбрать режим (Браузер)", Icon.host, () -> {
            dialog.hide();
            showModesMenu();
        });
        
        dialog.addCloseButton();
        dialog.show();
    }

    private void showModesMenu() {
        Dialog dialog = new Dialog("Выбор режима");
        Table t = dialog.cont;
        t.defaults().size(200f, 60f).pad(10f);

        String[] modes = {"Выживание", "Атака", "ПВП", "Песочница", "Аркада"};
        
        for (String mode : modes) {
            t.button(mode, () -> {
                dialog.hide();
                showMapList(mode);
            });
            t.row();
        }
        
        dialog.addCloseButton();
        dialog.show();
    }

    private void showMapList(String mode) {
        Dialog dialog = new Dialog("Режим: " + mode);
        Table listTable = new Table();
        dialog.cont.pane(listTable).size(600f, 500f);

        listTable.add("[accent]Загрузка списка карт...[]").pad(20f);

        NetCoreClient.fetchMaps(mode, (JsonValue maps) -> {
            listTable.clear();
            
            if (maps.size == 0) {
                listTable.add("[lightgray]В этом режиме пока нет карт. Будь первым![]").pad(20f);
                return;
            }

            for (JsonValue map : maps) {
                Table mapCard = new Table(Styles.black6).margin(10f);
                
                int id = map.getInt("id");
                String mapName = map.getString("name");
                String author = map.getString("author");
                String difficulty = map.getString("difficulty"); 
                int stars = map.getInt("stars");

                String starString = "";
                for(int s = 0; s < stars; s++) starString += "[accent][]"; 

                mapCard.add("[white]" + mapName + " [][lightgray]от " + author + "[]").left().row();
                mapCard.add("Сложность: [orange]" + difficulty + "[] | Оценка: " + (starString.isEmpty() ? "[gray]Нет[]" : starString)).left();
                
                mapCard.button(Icon.eye, () -> {
                    showMapDetails(map);
                }).right().padLeft(20f);

                listTable.add(mapCard).width(550f).pad(5f).row();
            }
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private void showMapDetails(JsonValue map) {
        int mapId = map.getInt("id");
        String name = map.getString("name");
        String author = map.getString("author");
        String diff = map.getString("difficulty");
        int stars = map.getInt("stars");
        int downloads = map.getInt("downloads");
        int plays = map.getInt("plays");
        String description = map.getString("description");

        Dialog dialog = new Dialog(name);
        Table t = dialog.cont;
        
        t.add("Автор: [lightgray]" + author + "[]").left().row();
        t.add("Сложность: [orange]" + diff + "[]").left().row();
        t.add("Скачиваний: [accent]" + downloads + "[] | Заходов: [accent]" + plays + "[]").left().padBottom(15f).row();
        t.add("[lightgray]" + description + "[]").width(450f).wrap().padBottom(20f).row();

        t.button(Icon.settings, () -> {
            showAdminAuthDialog(mapId, dialog);
        }).size(40f).padBottom(10f).row();

        Table buttons = new Table();
        buttons.defaults().size(140f, 50f).pad(10f);
        
        buttons.button("Скачать", Icon.download, () -> {
            NetCoreClient.sendAction(mapId, "download");
            Vars.ui.showInfo("Карта скачана!");
        });
        
        buttons.button("Играть", Icon.play, () -> {
            NetCorePlayer.playMap(map);
        });
        
        t.add(buttons);
        dialog.addCloseButton();
        dialog.show();
    }

    private void showAdminAuthDialog(int mapId, Dialog parentDetailsDialog) {
        Dialog auth = new Dialog("Панель модератора");
        String savedToken = Core.settings.getString("netcore-admin-token", "");
        
        auth.cont.add("Введи секретный токен админа:").row();
        var field = auth.cont.field(savedToken, text -> {}).width(300f).row().get();
        
        auth.button("Войти в панель", () -> {
            String enteredToken = field.getText();
            Core.settings.put("netcore-admin-token", enteredToken);
            auth.hide();
            showAdminEditDialog(enteredToken, mapId, parentDetailsDialog);
        }).size(200f, 50f).pad(10f);
        
        auth.addCloseButton();
        auth.show();
    }

    private void showAdminEditDialog(String token, int mapId, Dialog parentDetailsDialog) {
        Dialog edit = new Dialog("Управление картой #" + mapId);
        Table t = edit.cont;

        String[] difficulties = {"Safe", "Minimal", "Low", "Moderate", "Medium", "High", "Critical", "Extreme", "Eradication", "Unreasonable"};
        
        final AtomicReference<String> selectedDiff = new AtomicReference<>("Medium");
        final AtomicInteger selectedStars = new AtomicInteger(0);

        t.add("Выбери сложность:").row();
        Table diffTable = new Table();
        for(String d : difficulties) {
            diffTable.button(d, () -> {
                selectedDiff.set(d);
                Vars.ui.showInfoToast("Выбрано: " + d, 1f);
            }).size(110f, 40f).pad(2f);
            if(diffTable.getChildren().size % 3 == 0) diffTable.row();
        }
        t.add(diffTable).padBottom(15f).row();

        t.add("Выбери качество (Звезды):").row();
        Table starsTable = new Table();
        for(int i = 0; i <= 3; i++) {
            int finalI = i;
            starsTable.button(String.valueOf(i) + " ", () -> {
                selectedStars.set(finalI);
                Vars.ui.showInfoToast("Выбрано звезд: " + finalI, 1f);
            }).size(60f, 40f).pad(5f);
        }
        t.add(starsTable).padBottom(20f).row();

        edit.button("Применить изменения", Icon.ok, () -> {
            NetCoreClient.adminReview(token, mapId, selectedDiff.get(), selectedStars.get(), () -> {
                edit.hide();
                parentDetailsDialog.hide();
            });
        }).size(250f, 50f);

        edit.addCloseButton();
        edit.show();
    }
            }
