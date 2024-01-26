import Sox9_PV_CFos_Tools.Tools;
import ij.*;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Date;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;


/**
* Detect Sox9 cells and give their volume and intensity in Sox9 and CFos channel
* Detect PV cells and give their volume and intensity in PV and CFos channel
* @author ORION-CIRB
*/
public class Sox9_PV_CFos implements PlugIn {

    private Sox9_PV_CFos_Tools.Tools tools = new Tools();
       
    public void run(String arg) {
        try {
            if (!tools.checkInstalledModules() || !tools.checkStardistModels(tools.stardistModel)) {
                return;
            }
            
            String imageDir = IJ.getDirectory("Choose images directory")+File.separator;
            if (imageDir == null) {
                return;
            }
            
            // Find images with fileExt extension
            String fileExt = tools.findImageType(imageDir);
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);
            if (imageFiles.isEmpty()) {
                IJ.showMessage("Error", "No images found with " + fileExt + " extension");
                return;
            }
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);
            
            // Find channel names
            String[] channelNames = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Generate dialog box
            String[] channels = tools.dialog(imageDir, channelNames);
            if (channels == null) {
                IJ.showStatus("Plugin canceled");
                return;
            }
            
            // Create output folder
            String outDirResults = imageDir + File.separator + "Results_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            // Write headers results for results files
            FileWriter fwResultsSox9 = new FileWriter(outDirResults + "resultsSox9.csv", false);
            BufferedWriter resultsSox9 = new BufferedWriter(fwResultsSox9);
            resultsSox9.write("Image name\tImage vol (µm3)\tSox9 bg\tCFos bg\tSox9 cell label\tCell vol (µm3)"
                         + "\tSox9 bg-corrected mean int\tSox9 bg-corrected integrated int"
                         + "\tCFos bg-corrected mean int\tCFos bg-corrected integrated int\n");
            resultsSox9.flush();
            FileWriter fwResultsPv = new FileWriter(outDirResults + "resultsPV.csv", false);
            BufferedWriter resultsPv = new BufferedWriter(fwResultsPv);
            resultsPv.write("Image name\tImage vol (µm3)\tPV bg\tCFos bg\tPV cell label\tCell vol (µm3)"
                         + "\tPV bg-corrected mean int\tPV bg-corrected integrated int"
                         + "\tCFos bg-corrected mean int\tCFos bg-corrected integrated int\n");
            resultsPv.flush();
            
            for (String f: imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                
                // Analyze Sox9 channel
                tools.print("- Analyzing Sox9 channel -");
                int indexCh = ArrayUtils.indexOf(channelNames, channels[0]);
                ImagePlus imgSox9 = BF.openImagePlus(options)[indexCh];
                double bgSox9 = tools.findBackground(imgSox9);
                Objects3DIntPopulation popSox9 = tools.stardistDetection(imgSox9, tools.stardistProbThreshSox9, tools.minVolSox9, tools.maxVolSox9);
                
                // Analyze CFos channel
                tools.print("- Analyzing CFos channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[1]);
                ImagePlus imgCfos = BF.openImagePlus(options)[indexCh];
                double bgCfos = tools.findBackground(imgCfos);
                
                // Analyze PV channel
                tools.print("- Analyzing PV channel -");
                indexCh = ArrayUtils.indexOf(channelNames, channels[2]);
                ImagePlus imgPv = BF.openImagePlus(options)[indexCh];
                double bgPv = tools.findBackground(imgPv);
                Objects3DIntPopulation popPv = tools.stardistDetection(imgPv, tools.stardistProbThreshPv, tools.minVolPv, tools.maxVolPv);
                
                // Write results
                tools.print("- Writing and drawing results -");
                double imgVol = imgSox9.getWidth() * imgSox9.getHeight() * imgSox9.getNSlices() * tools.pixVol;
                for(Object3DInt cell: popSox9.getObjects3DInt()) {
                    double volUnit = new MeasureVolume(cell).getVolumeUnit();
                    double volPix = new MeasureVolume(cell).getVolumePix();
                    double corrMeanInt = new MeasureIntensity(cell, ImageHandler.wrap(imgSox9)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - bgSox9;
                    double corrIntInt = new MeasureIntensity(cell, ImageHandler.wrap(imgSox9)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - bgSox9*volPix;
                    double cfosCorrMeanInt = new MeasureIntensity(cell, ImageHandler.wrap(imgCfos)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - bgCfos;
                    double cfosCorrIntInt = new MeasureIntensity(cell, ImageHandler.wrap(imgCfos)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - bgCfos*volPix;
                    resultsSox9.write(rootName+"\t"+imgVol+"\t"+bgSox9+"\t"+bgCfos+"\t"+cell.getLabel()+"\t"+volUnit+"\t"+corrMeanInt+"\t"+corrIntInt+"\t"+cfosCorrMeanInt+"\t"+cfosCorrIntInt+"\n");                                
                    resultsSox9.flush();
                }
                for(Object3DInt cell: popPv.getObjects3DInt()) {
                    double volUnit = new MeasureVolume(cell).getVolumeUnit();
                    double volPix = new MeasureVolume(cell).getVolumePix();
                    double corrMeanInt = new MeasureIntensity(cell, ImageHandler.wrap(imgPv)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - bgPv;
                    double corrIntInt = new MeasureIntensity(cell, ImageHandler.wrap(imgPv)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - bgPv*volPix;
                    double cfosCorrMeanInt = new MeasureIntensity(cell, ImageHandler.wrap(imgCfos)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) - bgCfos;
                    double cfosCorrIntInt = new MeasureIntensity(cell, ImageHandler.wrap(imgCfos)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM) - bgCfos*volPix;
                    
                    resultsPv.write(rootName+"\t"+imgVol+"\t"+bgPv+"\t"+bgCfos+"\t"+cell.getLabel()+"\t"+volUnit+"\t"+corrMeanInt+"\t"+corrIntInt+"\t"+cfosCorrMeanInt+"\t"+cfosCorrIntInt+"\n");                                
                    resultsPv.flush();
                }
                
                // Draw results
                tools.drawResults(popSox9, imgSox9, imgCfos, true, outDirResults+rootName+"_Sox9.tif");
                tools.drawResults(popPv, imgPv, imgCfos, false, outDirResults+rootName+"_PV.tif");
                
                tools.closeImage(imgSox9);
                tools.closeImage(imgCfos);
                tools.closeImage(imgPv);
            }
            resultsSox9.close();
            resultsPv.close();
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(Sox9_PV_CFos.class.getName()).log(Level.SEVERE, null, ex);
        }
        tools.print("All done!");
    }
}
