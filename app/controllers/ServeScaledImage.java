package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.lang.Integer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import play.mvc.Controller;
import play.mvc.Result;
import play.Play;

import javax.imageio.*;
import static org.imgscalr.Scalr.*;

public class ServeScaledImage extends Controller {
    public static Result at(Integer size, String filename) {
        response().setContentType("image");
        ByteArrayOutputStream img_stream;
        try {
            String[] parts = filename.split("\\.", -1);
            if (parts.length == 1) parts[1] = ".jpg";
            File file = new File(Play.application().configuration().getString("pictures_parDir")+size+"/"+filename);
            if (file.exists() && !file.isDirectory()) {
                BufferedImage thumbnail = ImageIO.read(file);
                img_stream = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, parts[1], img_stream);
            } else {
                File origFile = new File(Play.application().configuration().getString("pictures_dir")+filename);
                BufferedImage thumbnail = createThumbnail(ImageIO.read(origFile), size, origFile, parts[1]);
                img_stream = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, parts[1], img_stream);
            }
        } catch (FileNotFoundException e) {
            return badRequest("image not found");
        } catch (IOException e) {
            return internalServerError("unknown server error");
        }
        return ok(img_stream.toByteArray());
    }

    public static BufferedImage createThumbnail(BufferedImage img, Integer toSize, File file, String ext) {
        if (img.getWidth() > toSize || img.getHeight() > toSize)
            img = resize(img, Method.ULTRA_QUALITY, toSize, OP_ANTIALIAS, OP_BRIGHTER);
        String imgPath = file.getParentFile().getParent()+"/"+toSize+"/"+file.getName();
        try {
            ImageIO.write(img, ext, new File(imgPath));
        } catch (IOException e) {
            System.out.println("there was an error while saving a scaled image");
        }
        return img; //pad(img, 4);
    }
    public static boolean saveScaledImage(List<Integer> toSizes, File file) {
        try {
            String ext = FilenameUtils.getExtension(file.getAbsolutePath());
            BufferedImage img = ImageIO.read(file);
            for(Iterator<Integer> i = toSizes.iterator(); i.hasNext();) {
                createThumbnail(img, i.next(), file, ext);
            }
        }
        catch (IOException e) {
            System.out.println("there was an error while saving a scaled image");
        }
        return true;
    }
}