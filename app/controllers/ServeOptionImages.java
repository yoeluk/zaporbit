package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import play.mvc.Controller;
import play.mvc.Result;
import play.Play;

import javax.imageio.*;

public class ServeOptionImages extends Controller {
    public static Result at(String filename) {
        response().setContentType("image");
        ByteArrayOutputStream img_stream = null;
        try {
            File file = new File(Play.application().configuration().getString("options_dir")+filename);
            BufferedImage thumbnail = ImageIO.read(file);
            img_stream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", img_stream);
        } catch (FileNotFoundException e) {
            return badRequest("image not found");
        } catch (IOException e) {
            return internalServerError("unknown server error");
        }
        return ok(img_stream.toByteArray());
    }
}
