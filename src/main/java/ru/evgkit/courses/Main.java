package ru.evgkit.courses;

import ru.evgkit.courses.model.CourseIdea;
import ru.evgkit.courses.model.CourseIdeaDAO;
import ru.evgkit.courses.model.SimpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main {
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
            return new ModelAndView(model, "index.hbs");
        }, new HandlebarsTemplateEngine());

        post("/sign-in", (request, response) -> {
            response.cookie("username", request.queryParams("username"));
            response.redirect("/");
            return null;
        });

        before("/ideas", (request, response) -> {
            if (null == request.attribute("username")) {
                response.redirect("/");
            }
        });

        get("/ideas", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("ideas", courseIdeaDAO.findAll());

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
            String slug = request.params("slug");
            CourseIdea idea = courseIdeaDAO.findBySlug(slug);
            idea.addVoter(request.attribute("username"));
            response.redirect("/ideas/" + slug);
            return null;
        });
    }
}
