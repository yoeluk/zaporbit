package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.imgscalr.Scalr;
import play.mvc.Controller;
import play.mvc.Result;
import play.Play;

import javax.imageio.*;

import static org.imgscalr.Scalr.OP_ANTIALIAS;
import static org.imgscalr.Scalr.OP_BRIGHTER;
import static org.imgscalr.Scalr.resize;

public class ServeOptionImages extends Controller {
    public static Result at(Integer size, String filename) {
        response().setContentType("image");
        ByteArrayOutputStream img_stream;
        try {
            String[] parts = filename.split("\\.", -1);
            File file = new File(Play.application().configuration().getString("options_dir")+filename);
            BufferedImage thumbnail = createThumbnail(ImageIO.read(file), size);
            img_stream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, parts[1], img_stream);
        } catch (FileNotFoundException e) {
            return badRequest("image not found");
        } catch (IOException e) {
            return internalServerError("unknown server error");
        }
        return ok(img_stream.toByteArray());
    }

    public static BufferedImage createThumbnail(BufferedImage img, Integer size) {
        if (img.getWidth() > size || img.getHeight() > size)
            img = resize(img, Scalr.Method.ULTRA_QUALITY, size, OP_ANTIALIAS, OP_BRIGHTER);
        return img; //pad(img, 4);
    }
}
