package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO
import java.awt.Transparency
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.system.exitProcess


class WaterMarkApp() {
    private val transparencyTypes = mutableListOf<String>("OPAQUE", "BITMASK", "TRANSLUCENT")
    private var inputFile = File(".")
    private var inputImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    private var watermarkFile = File(".")
    private var watermarkImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    private var outputFile = File(".")
    private var outputImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    private var isAlpha = false
    private var hasTransparentColor = false
    private var transparentColor = Color(0, 0, 0)
    private var dx = 0
    private var dy = 0

    private fun showInfo() {
        println("Image file: ${inputFile.path}")
        println("Width: ${inputImage.width}")
        println("Height: ${inputImage.height}")
        println("Number of components: ${inputImage.colorModel.numComponents}")
        println("Number of color components: ${inputImage.colorModel.numColorComponents}")
        println("Bits per pixel: ${inputImage.colorModel.pixelSize}")
        println("Transparency: ${transparencyTypes[inputImage.transparency - 1]}")
    }

    private fun checkInputImage() {
        if (inputImage.colorModel.numColorComponents != 3) {
            println("The number of image color components isn't 3.")
            exitProcess(1)
        }

        if (inputImage.colorModel.pixelSize !in listOf<Int>(24, 32)) {
            println("The image isn't 24 or 32-bit.")
            exitProcess(1)
        }
    }

    private fun checkWaterMarkImage() {
        if (watermarkImage.colorModel.numColorComponents != 3) {
            println("The number of watermark color components isn't 3.")
            exitProcess(1)
        }

        if (watermarkImage.colorModel.pixelSize !in listOf<Int>(24, 32)) {
            println("The watermark isn't 24 or 32-bit.")
            exitProcess(1)
        }
    }

    fun prepareImage() {
        outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until inputImage.height) {
            for (x in 0 until inputImage.width) {
                val i = Color(inputImage.getRGB(x, y))
                outputImage.setRGB(x, y, i.rgb)
            }
        }
    }

    private fun blendImages(weight: Int) {
        var currWeight: Int
        for (y in 0 until watermarkImage.height) {
            for (x in 0 until watermarkImage.width) {

                if (x + dx < inputImage.width && y + dy < inputImage.height) {
                    val i = Color(inputImage.getRGB(x + dx, y + dy))
                    var w: Color
                    if (isAlpha) {
                        w = Color(watermarkImage.getRGB(x, y), true)
                        if (w.alpha == 0) {
                            currWeight = 0
                        } else {
                            currWeight = weight
                        }
                    } else {
                        if (hasTransparentColor) {
                            w = Color(watermarkImage.getRGB(x, y))
                            if (w == transparentColor) {
                                currWeight = 0
                            } else {
                                currWeight = weight
                            }
                        } else {
                            w = Color(watermarkImage.getRGB(x, y))
                            currWeight = weight
                        }
                    }


                    val color = Color(
                        (currWeight * w.red + (100 - currWeight) * i.red) / 100,
                        (currWeight * w.green + (100 - currWeight) * i.green) / 100,
                        (currWeight * w.blue + (100 - currWeight) * i.blue) / 100
                    )

                    outputImage.setRGB(x + dx, y + dy, color.rgb)

                }
            }
        }
    }

    private fun multipleBlendImages(weight: Int) {
        while (dy < inputImage.height) {
            while (dx < inputImage.width) {
                blendImages(weight)
                dx += watermarkImage.width
            }
            dx = 0
            dy += watermarkImage.height
        }
        dx = 0
        dy = 0
    }



    fun applyWaterMark() {

        // try access input image
        println("Input the image filename:")
        try {
            val pathName = readln()
            inputFile = File(pathName)
            inputImage = ImageIO.read(inputFile)
            checkInputImage()


        } catch (e: Exception) {
            println("The file ${inputFile.path} doesn't exist.")
            exitProcess(1)
        }

        // try access watermark image
        println("Input the watermark image filename:")
        try {
            val pathName = readln()
            watermarkFile = File(pathName)
            watermarkImage = ImageIO.read(watermarkFile)
            checkWaterMarkImage()


        } catch (e: Exception) {
            println("The file ${watermarkFile.path} doesn't exist.")
            exitProcess(1)
        }

        // check sizes
        if (inputImage.width < watermarkImage.width || inputImage.height < watermarkImage.height) {
            println("The watermark's dimensions are larger.")
            exitProcess(1)
        }

        // ask for transparency behaviour
        if (transparencyTypes[watermarkImage.transparency - 1] == "TRANSLUCENT") {
            println("Do you want to use the watermark's Alpha channel?")
            val input = readln().lowercase()
            if (input == "yes") {
                isAlpha = true
            }
        } else {
            println("Do you want to set a transparency color?")
            val input = readln().lowercase()
            if (input == "yes") {
                try {
                    println("Input a transparency color ([Red] [Green] [Blue]):")
                    val inputColor = readln().lowercase()
                    val format = Regex("""(\d+)\s(\d+)\s(\d+)""")
                    val match = format.find(inputColor)
                    if (inputColor.matches(format)) {
                        hasTransparentColor = true
                        if (match != null) {
                            val red = match.groupValues[1].toInt()
                            val green = match.groupValues[2].toInt()
                            val blue = match.groupValues[3].toInt()
                            if (red !in 0..255 || green !in 0..255 || blue !in 0..255) {
                                throw Exception()
                            }
                            transparentColor = Color(red, green, blue)
                        }
                    } else {
                        println("The transparency color input is invalid.")
                        exitProcess(1)
                    }
                } catch (e: Exception) {
                    println("The transparency color input is invalid.")
                    exitProcess(1)
                }
            }
        }

        println("Input the watermark transparency percentage (Integer 0-100):")
        val weightString = readln()
        var weight = 0
        try {
            weight = weightString.toInt()
            if (weight !in 1..100) throw IllegalStateException()
        } catch (e: NumberFormatException) {
            println("The transparency percentage isn't an integer number.")
            exitProcess(1)
        } catch (e: IllegalStateException) {
            println("The transparency percentage is out of range.")
            exitProcess(1)
        }

        // ask for mode of application
        println("Choose the position method (single, grid):")
        val method = readln()
        if (method == "single") {
            val format = Regex("""(-?\d+)\s(-?\d+)""")
            val dxMax = inputImage.width - watermarkImage.width
            val dyMax = inputImage.height - watermarkImage.height
            try {
                println("Input the watermark position ([x 0-$dxMax] [y 0-$dyMax]):")
                val input = readln()
                val match = format.find(input)
                if (input.matches(format)) {
                    if (match != null) {
                        dx = match.groupValues[1].toInt()
                        dy = match.groupValues[2].toInt()
                        if (dx !in 0..dxMax || dy !in 0..dyMax) {
                            throw Exception()
                        }

                    }
                } else {
                    println("The position input is invalid.")
                    exitProcess(1)
                }
            } catch (e: Exception) {
                println("The position input is out of range.")
                exitProcess(1)
            }

        } else if (method == "grid") {
            dx = 0
            dy = 0
        } else {
            println("The position method input is invalid.")
            exitProcess(1)
        }

        // ask for output image name
        println("Input the output image filename (jpg or png extension):")
        try {
            val pathName = readln()
            outputFile = File(pathName)
            if (outputFile.extension !in listOf<String>("jpg", "png")) {
                throw IllegalStateException()
            }
        } catch (e: IllegalStateException) {
            println("The output file extension isn't \"jpg\" or \"png\".")
            exitProcess(1)
        }


        prepareImage()
        // apply blending
        if (method == "single") {
            blendImages(weight)
        } else if (method == "grid") {
            multipleBlendImages(weight)
        }

        ImageIO.write(outputImage, outputFile.extension, outputFile)
        println("The watermarked image ${outputFile.path} has been created.")


    }
}

fun main() {
    val app = WaterMarkApp()
    app.applyWaterMark()
}