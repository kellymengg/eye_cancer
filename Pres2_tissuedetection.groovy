import qupath.lib.scripting.QP
import javax.imageio.ImageIO  // Import for ImageIO
import java.awt.image.BufferedImage
import java.io.File

// Set image type and color deconvolution stains
setImageType('BRIGHTFIELD_H_E')
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.65111 0.70119 0.29049", "Stain 2" : "Eosin", "Values 2" : "0.2159 0.8012 0.5581", "Background" : " 255 255 255"}')

// Reset selection and create annotations from pixel classifier
resetSelection()
createAnnotationsFromPixelClassifier("Tissue detection", 100000.0, 0.0, "SPLIT")

// Get the current image data
def imageData = QP.getCurrentImageData()

// Get all annotations
def annotations = QP.getAnnotationObjects()

// Set names for each annotation based on their classification
annotations.each { annotation ->
    def classification = annotation.getPathClass()?.getName() // Get the name of the class
    if (classification) {
        annotation.setName(classification)
    } else {
        annotation.setName("Unclassified") // Fallback name for any unclassified annotations
    }
    
    println("Annotation: ${annotation.getName()}")
}

// Filter annotations to include only those named "Tissue"
def tissueAnnotations = annotations.findAll { annotation ->
    annotation.getName() == "Tissue"
}

// Define the output directory
def outputDir = '/Users/annie/Desktop/qupath/export'  // Update this to your desired path

// Ensure output directory exists
def outputDirFile = new File(outputDir)
if (!outputDirFile.exists()) {
    outputDirFile.mkdirs()  // Create the directory if it doesn't exist
}

// Set batch size
int batchSize = 2  // Adjust this value based on your memory constraints

// Export each tissue annotation in batches
for (int i = 0; i < tissueAnnotations.size(); i += batchSize) {
    // Get the current batch of tissue annotations
    def batch = tissueAnnotations[i..Math.min(i + batchSize - 1, tissueAnnotations.size() - 1)]
    
    // Export each annotation in the batch
    batch.eachWithIndex { annotation, index ->
        def outputPath = "${outputDir}/Tissue_Annotation_${i + index + 1}.tiff"
        
        // Get the server and region of interest
        def server = imageData.getServer()
        def roi = annotation.getROI()
        
        // Create a RegionRequest and read the region
        def regionRequest = RegionRequest.createInstance(server.getPath(), 1.0, roi)
        
        // Read the region as a BufferedImage
        BufferedImage bufferedImage = server.readRegion(regionRequest)

        // Save the buffered image as TIFF
        try {
            ImageIO.write(bufferedImage, "TIFF", new File(outputPath))
            println("Exported annotation ${i + index + 1} to: ${outputPath}")
        } catch (IOException e) {
            println("Failed to export annotation ${i + index + 1}: ${e.message}")
        }
    }
}