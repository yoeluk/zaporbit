package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.lang.reflect.Array;

import org.apache.commons.io.output.ByteArrayOutputStream;
import play.mvc.Controller;
import play.mvc.Result;
import play.Play;

import javax.imageio.*;

public class ServeImage extends Controller {
    public static Result at(String filename) {
        response().setContentType("image");
        ByteArrayOutputStream img_stream = null;
        try {
            String[] parts = filename.split("\\.", -1);
            File file = new File(Play.application().configuration().getString("pictures_dir")+filename);
            BufferedImage thumbnail = ImageIO.read(file);
            img_stream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, parts[1], img_stream);
        } catch (FileNotFoundException e) {
            return badRequest("image not found");
        } catch (IOException e) {
            return internalServerError("unknown server error");
        }
        return ok(img_stream.toByteArray());
    }
}