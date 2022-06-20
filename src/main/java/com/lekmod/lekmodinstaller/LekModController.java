package com.lekmod.lekmodinstaller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;


public class LekModController {
    @FXML
    ProgressIndicator p1 = new ProgressIndicator();
    @FXML
    DirectoryChooser fileChooser = new DirectoryChooser();

    @FXML
    Text fileLocation = new Text();

    @FXML
    Text currentLekMod = new Text();

    @FXML
    Text currentLekMap = new Text();

    @FXML
    CheckBox Lekmodcheckbox = new CheckBox();

    @FXML
    CheckBox Lekmapcheckbox = new CheckBox();

    @FXML
    MenuButton dropDownVersion = new MenuButton();

    ArrayList<VersionInfo> version = new ArrayList<>();

    Task<Void> moveLekMod;

    Task<Void> moveLekMap;

    boolean errorLekMod = false;

    boolean errorLekMap = false;

    @FXML
    public void initialize() {
        File CivCDrive = new File(System.getenv("ProgramFiles(X86)") + "\\Steam\\steamapps\\common\\Sid Meier's Civilization V\\Assets");
        File CivDDrive = new File("D:\\SteamLibrary\\steamapps\\common\\Sid Meier's Civilization V\\Assets");
        fileChooser.setTitle("Civ V Assets");
        if (CivCDrive.exists()) {
            fileChooser.setInitialDirectory(CivCDrive);
            fileLocation.setText("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Sid Meier's Civilization V\\Assets");
        } else if (CivDDrive.exists()) {
            fileChooser.setInitialDirectory(CivDDrive);
            fileLocation.setText("D:\\SteamLibrary\\steamapps\\common\\Sid Meier's Civilization V\\Assets");
        } else
            fileLocation.setText("<- Select Assets Folder");

        try {
            fetchData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkVersion();

    }

    private void checkVersion() {
        if (!fileLocation.getText().equalsIgnoreCase("<- Select Assets Folder")) {
            File dlcFolder = new File(fileLocation.getText() + "\\DLC\\");
            File mapsFolder = new File(fileLocation.getText() + "\\Maps\\");
            // if dlcfolder contains a folder called lekmod anycase
            boolean setLekmod = false;
            if (dlcFolder.exists() && dlcFolder.isDirectory()) {
                File[] files = dlcFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().toLowerCase().contains("lekmod")) {
                            // get only the version number and "."
                            String versionNumber = file.getName().replaceAll("[^0-9.]", "");
                            // set current lekmod to the version number
                            if (versionNumber.length() < 1)
                                versionNumber = "???";
                            currentLekMod.setText("V" + versionNumber);
                            setLekmod = true;
                            break;
                        }
                    }
                }
            }
            if (!setLekmod)
                currentLekMod.setText("No");

            // do the same for maps
            boolean setLekmap = false;
            if (mapsFolder.exists() && mapsFolder.isDirectory()) {
                File[] files = mapsFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().toLowerCase().contains("lekmap")) {
                            String versionNumber = file.getName().replaceAll("[^0-9.]", "");
                            if (versionNumber.length() < 1)
                                versionNumber = "???";
                            currentLekMap.setText("V" + versionNumber);
                            setLekmap = true;
                            break;
                        }
                    }
                }
            }
            if (!setLekmap)
                currentLekMap.setText("No");
        }
    }


    // fetch json data from "https://api.github.com/repos/EnormousApplePie/Lekmod/tags"
    private void fetchData() throws Exception {
        String sURL = "https://api.github.com/repos/EnormousApplePie/Lekmod/tags";

        // Connect to the URL using java's native library
        URL url = new URL(sURL);
        URLConnection request = url.openConnection();
        request.connect();

        // Convert to a JSON object to print data

        // pull JSON data from the URL
        JsonArray jsonParser = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())).getAsJsonArray();
        for (JsonElement jsonElement : jsonParser) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String name = jsonObject.get("name").getAsString();
            String commit = jsonObject.get("zipball_url").getAsString();
            dropDownVersion.getItems().add(new MenuItem(name));
            version.add(new VersionInfo(Double.parseDouble(name.substring(1)), commit));
        }

        dropDownVersion.setText("V" +
                version.stream().mapToDouble(versionInfo -> versionInfo.version).filter(versionInfo -> versionInfo >= 0).max().orElse(0));

        EventHandler<ActionEvent> changeVersion = e -> {
            String versionNumber = ((MenuItem) e.getSource()).getText().substring(1);
            dropDownVersion.setText("V" + versionNumber);
        };

        dropDownVersion.getItems().forEach(item -> item.setOnAction(changeVersion));
    }

    // fired when user clicks "Directory" button
    public void file(ActionEvent event) {
        File selectedFile = fileChooser.showDialog(null);
        if (selectedFile != null) {
            fileLocation.setText(selectedFile.getAbsolutePath());
            checkVersion();
        }
    }

    // fired when user clicks "Install" button
    public void install(ActionEvent event) {
        if (!fileLocation.getText().equalsIgnoreCase("<- Select Assets Folder")) {
            if (Lekmodcheckbox.isSelected() && currentLekMod != null && !currentLekMod.getText().equalsIgnoreCase("No")) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Found Lekmod");
                alert.setHeaderText("Lekmod is already installed");
                alert.setContentText("Installing Lekmod will overwrite the current version of Lekmod. "
                        + currentLekMod.getText() + "\n Are you sure you want to continue?");
                alert.showAndWait();
                if (alert.getResult() == ButtonType.CANCEL)
                    return;
            }
            if (Lekmapcheckbox.isSelected() && currentLekMap != null && !currentLekMap.getText().equalsIgnoreCase("No")) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Found Lekmap");
                alert.setHeaderText("Lekmap is already installed");
                alert.setContentText("Installing Lekmap will overwrite the current version of Lekmap. "
                        + currentLekMap.getText() + "\n Are you sure you want to continue?");
                alert.showAndWait();
                if (alert.getResult() == ButtonType.CANCEL)
                    return;
            }

            downloadAndUnzip();
        }

    }

    private void downloadAndUnzip() {
        if (Lekmodcheckbox.isSelected() || Lekmapcheckbox.isSelected()) {

            String versionNumber = dropDownVersion.getText().substring(1);
            String sURL = version.stream().filter(versionInfo ->
                    versionInfo.version == Double.parseDouble(versionNumber)).findFirst().map(versionInfo -> versionInfo.url).orElse(null);

            if (sURL != null) {
                p1.setProgress(0.01);
                Task<Void> download = new Task<>() {
                    @Override
                    public Void call() {
                       /* try {
                            double progress = 0.01;
                            updateProgress(progress, 1);
                            URL url = new URL(sURL);
                            URLConnection request = url.openConnection();
                            request.connect();
                            InputStream input = new BufferedInputStream(request.getInputStream());
                            FileOutputStream output = new FileOutputStream(fileLocation.getText() + "\\Lekmap.zip");
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                                double ContentLength = request.getContentLengthLong();
                                if (ContentLength < 5)
                                    ContentLength = 500000000;
                                progress = progress + (bytesRead / ContentLength);
                                updateProgress(progress / 2, 1);
                                output.write(buffer, 0, bytesRead);
                            }
                            input.close();
                            output.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        */
                        return null;
                    }
                };
                p1.progressProperty().bind(download.progressProperty());

                new Thread(download).start();
                download.setOnSucceeded(e -> {
                    unZip();
                });
            }
        }
    }

    private void unZip() {
        // once the task is done, unzip the file

        Task<Void> unzip = new Task<>() {
            @Override
            public Void call() {
                            /*
                            try {
                                ZipFile zipFile = new ZipFile(fileLocation.getText() + "\\Lekmap.zip");
                                zipFile.extractAll(fileLocation.getText());
                                zipFile.close();
                                Files.delete(Paths.get(fileLocation.getText() + "\\Lekmap.zip"));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                             */
                return null;
            }
        };
        new Thread(unzip).start();
        p1.progressProperty().unbind();

        unzip.setOnSucceeded(e2 -> {
            File path = null;

            // in filelocation, find the folder "lekmod"
            File lekmodFolder = new File(fileLocation.getText());
            File[] files = lekmodFolder.listFiles();
            if (files != null)
                for (File assetsFolder : files) {
                    if (assetsFolder.isDirectory() && assetsFolder.getName().toLowerCase().contains("lekmod")) {
                        path = assetsFolder;
                        break;
                    }
                }

            p1.setProgress(.75);

            moveLekModFolder(path);
            moveLekMapFolder(path);

            File finalPath1 = path;
            if (Lekmodcheckbox.isSelected()) {
                moveLekMod.setOnSucceeded(e3 -> {
                    doRename();
                    if (Lekmapcheckbox.isSelected()) {
                        CompleteInstall(finalPath1);
                    } else
                        CompleteInstall2(finalPath1);

                });
            } else if (Lekmapcheckbox.isSelected()) {
                CompleteInstall(finalPath1);
            }
        });
    }

    // rename to include the version number :)
    private void doRename() {
        if (fileLocation.getText() != null) {
            File Lekmod = new File(fileLocation.getText() + "\\DLC\\LEKMOD");
            File Lekmodv = new File(fileLocation.getText() + "\\DLC\\LEKMOD_" + dropDownVersion.getText());
            if (!Lekmodv.exists())
                if (Lekmod.exists())
                    Lekmod.renameTo(Lekmodv);

        }
    }


    private void moveLekModFolder(File path) {
        if (Lekmodcheckbox.isSelected() && path != null) {
            File[] files2 = path.listFiles();
            if (files2 != null)
                for (File lekmodDfolder : files2) {
                    // checking in lekmod which we unzipped for dlc
                    if (lekmodDfolder.isDirectory() && lekmodDfolder.getName().toLowerCase().contains("dlc")) {
                        moveLekMod = new Task<>() {
                            @Override
                            public Void call() {
                                try {
                                    File dlcFolder = new File(fileLocation.getText() + "\\DLC");
                                    FileUtils.copyDirectory(lekmodDfolder, dlcFolder);
                                } catch (IOException exception) {
                                    errorLekMod = true;
                                }
                                return null;
                            }
                        };
                        new Thread(moveLekMod).start();
                        break;
                    }
                }
        }
    }


    private void moveLekMapFolder(File path) {
        if (Lekmapcheckbox.isSelected() && path != null) {
            File[] files2 = path.listFiles();
            if (files2 != null)
                for (File file : files2) {
                    if (file.isDirectory() && file.getName().toLowerCase().contains("lekmap")) {
                        File mapsFolder = new File(fileLocation.getText() + "\\Maps");
                        moveLekMap = new Task<>() {
                            @Override
                            public Void call() {
                                try {
                                    FileUtils.copyDirectoryToDirectory(file, mapsFolder);
                                } catch (IOException ignored) {
                                    errorLekMap = true;
                                }
                                return null;
                            }
                        };
                        new Thread(moveLekMap).start();
                        break;
                    }
                }
        }
    }

    private void CompleteInstall(File finalPath1) {
        if (moveLekMap.isDone()) {
            CompleteInstall2(finalPath1);
        } else {
            moveLekMap.setOnSucceeded(e4 -> CompleteInstall2(finalPath1));
        }
    }

    private void CompleteInstall2(File finalPath1) {
        Alert alert;
        if (!errorLekMap && !errorLekMod) {
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Lek Installation Complete");
            alert.setHeaderText("Lek installation complete");
            alert.setContentText("Lek has been installed successfully. "
                    + "Please restart the game to load the new version.");
        } else {
            alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lek Installation Failed");
            alert.setHeaderText("Lek installation failed");
            alert.setContentText("Please Manually install Lek. ");
        }
        alert.showAndWait();
        checkVersion();
        if (finalPath1 != null) {
            deleteDirectory(finalPath1);
        }
        p1.setProgress(1);
    }

    public void uninstall(ActionEvent event) {
        if (!fileLocation.getText().equalsIgnoreCase("<- Select Assets Folder")) {
            if (Lekmodcheckbox.isSelected())
                uninstallLekmod();
            if (Lekmapcheckbox.isSelected())
                uninstallLekmap();
            checkVersion();
        }
    }

    private void uninstallLekmap() {
        if (uninstall("Lekmap", currentLekMap, "\\Maps\\")) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("lekmap removed");
            a.setHeaderText("lekmap removed");
            a.showAndWait();
        }
    }

    private void uninstallLekmod() {
        if (uninstall("Lekmod", currentLekMod, "\\DLC\\")) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("lekmod removed");
            a.setHeaderText("lekmod removed");
            a.showAndWait();
        }
    }

    private boolean uninstall(String name, Text mod, String location) {
        if (mod.getText().equalsIgnoreCase("No")) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText(name + " is not installed");
            a.setContentText("The current folder does not contain " + name);
            a.showAndWait();
            return false;
        } else {
            File dlcFolder = new File(fileLocation.getText() + location);
            if (dlcFolder.exists() && dlcFolder.isDirectory()) {
                File[] files = dlcFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().toLowerCase().contains(name.toLowerCase())) {
                            deleteDirectory(file);
                        }
                    }
                }
            }
            return true;
        }
    }

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null)
            Arrays.stream(allContents).forEach(this::deleteDirectory);
        directoryToBeDeleted.delete();
    }

    public void discordIcon(ActionEvent event) {
        String url = "https://discord.gg/VQBNPmc";
        openUrl(url);
    }

    public void githubIcon(ActionEvent event) {
        String url = "https://github.com/EnormousApplePie/Lekmod";
        openUrl(url);
    }

    private void openUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}