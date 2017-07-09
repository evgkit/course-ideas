package ru.evgkit.courses;

import ru.evgkit.courses.model.CourseIdea;
import ru.evgkit.courses.model.CourseIdeaDAO;
import ru.evgkit.courses.model.NotFoundException;
import ru.evgkit.courses.model.SimpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    private static final String FLASH_MESSAGE_KEY = "flash_message";

    public static void main(String[] args) {
        staticFileLocation("/public");

        CourseIdeaDAO courseIdeaDAO = new SimpleCourseIdeaDAO();

        before((request, response) -> {
            String username = request.cookie("username");
            if (null != username) {
                request.attribute("username", username);
            }
        });

        get("/", (request, response) -> {
            Map<String, String> model = new HashMap<>();
            model.put("username", request.attribute("username"));
            model.put("flashMessage", popupFlashMessageKey(request));

            return new ModelAndView(model, "index.hbs");
        }, new HandlebarsTemplateEngine());

        post("/sign-in", (request, response) -> {
            response.cookie("username", request.queryParams("username"));
            response.redirect("/");
            return null;
        });

        before("/ideas", (request, response) -> {
            if (null == request.attribute("username")) {
                setFlashMessage(request, "Whoops, please sign in first!");
                response.redirect("/");
                halt();
            }
        });

        get("/ideas", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("ideas", courseIdeaDAO.findAll());
            model.put("flashMessage", popupFlashMessageKey(request));

            return new ModelAndView(model, "ideas.hbs");
        }, new HandlebarsTemplateEngine());

        post("/ideas", (request, response) -> {
            courseIdeaDAO.add(new CourseIdea(
                    request.queryParams("title"),
                    request.attribute("username"))
            );
            response.redirect("/ideas");
            return null;
        });

        get("ideas/:slug", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("idea", courseIdeaDAO.findBySlug(request.params("slug")));
            return new ModelAndView(model, "idea.hbs");
        }, new HandlebarsTemplateEngine());

        post("/ideas/:slug/vote", (request, response) -> {
            CourseIdea idea = courseIdeaDAO.findBySlug(request.params("slug"));

            boolean isAdded = idea.addVoter(request.attribute("username"));
            setFlashMessage(request, isAdded ? "Your vote accepted!" : "You already voted!") ;

            response.redirect("/ideas");
            return null;
        });

        exception(NotFoundException.class, (exception, request, response) -> {
            response.status(404);
            HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
            String html = engine.render(new ModelAndView(null, "not-found.hbs"));
            response.body(html);
        });
    }

    private static void setFlashMessage(Request request, String message) {
        request.session().attribute(FLASH_MESSAGE_KEY, message);
    }

    private static String getFlashMessageKey(Request request) {
        if (request.session(false) == null) {
            return null;
        }
        if (!request.session().attributes().contains(FLASH_MESSAGE_KEY)) {
            return null;
        }
        return (String) request.session().attribute(FLASH_MESSAGE_KEY);
    }

    private static String popupFlashMessageKey(Request request) {
        String flashMessage = getFlashMessageKey(request);
        if (null != flashMessage) {
            request.session().removeAttribute(FLASH_MESSAGE_KEY);
        }
        return flashMessage;
    }
}
