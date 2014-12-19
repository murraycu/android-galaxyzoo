/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo.
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app;

import android.text.TextUtils;
import android.util.JsonReader;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by murrayc on 7/30/14.
 */
public class DecisionTree {

    private static final String NODE_ROOT = "murrayc_zoonverse_questions";
    private static final String NODE_QUESTION = "question";
    private static final String NODE_CHECKBOX = "checkbox";
    private static final String NODE_ANSWER = "answer";

    //TODO: Make this private and add accessors.
    public final Map<String, Question> questionsMap = new HashMap<>();

    /**
     * @param inputStreamTree        The XMl file containing the decision tree.
     * @param inputStreamTranslation A JSON file containing translations of the question and answers,
     *                               such as https://github.com/zooniverse/Galaxy-Zoo/blob/master/public/locales/es.json
     */
    public DecisionTree(final InputStream inputStreamTree, final InputStream inputStreamTranslation) throws DecisionTreeException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //Disable feature that we don't need and which just slows the parsing down:
        //TODO: Confirm that this actually makes a difference.
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);

        DocumentBuilder documentBuilder;
        try {
            documentBuilder = dbf.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            throw new DecisionTreeException("Exception from newDocumentBuilder." + e);
        }

        org.w3c.dom.Document xmlDocument;

        try {
            xmlDocument = documentBuilder.parse(inputStreamTree);
        } catch (final SAXException | IOException e) {
            throw new DecisionTreeException("Exception from DocumentBuilder.parse()." + e);
        }

        final Element rootNode = xmlDocument.getDocumentElement();
        if (!TextUtils.equals(rootNode.getNodeName(), NODE_ROOT)) {
            throw new DecisionTreeException("Unexpected XML root node name found: " + rootNode.getNodeName());
        }

        final List<Node> listQuestions = getChildrenByTagName(rootNode, NODE_QUESTION);
        for (final Node node : listQuestions) {
            if (!(node instanceof Element)) {
                continue;
            }

            final Element element = (Element) node;
            final Question question = loadQuestion(element);
            questionsMap.put(question.getId(), question);
        }

        //Load the translation if one was provided:
        //We don't avoid loading the English strings before,
        //because the translation might be incomplete.
        //TODO: Find an efficient way to avoid loading English strings that will be replaced,
        //maybe by loading the translation first.
        if (inputStreamTranslation != null) {
            try {
                loadTranslation(inputStreamTranslation);
            } catch (final IOException e) {
                throw new DecisionTreeException("loadTranslation() failed", e);
            }
        }
    }

    private void loadTranslation(final InputStream inputStreamTranslation) throws IOException {
        final JsonReader reader;
        try {
            reader = new JsonReader(new InputStreamReader(inputStreamTranslation, Utils.STRING_ENCODING));
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("questions")) { //We ignore the "zooniverse" and "quiz_questions" objects
                    readJsonQuestions(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
        } catch (final UnsupportedEncodingException e) {
            //This is very unlikely for UTF-8, so just ignore it.
            Log.error("DecisionTree: UnsupportedEncodingException parsing JSON", e);
        }
    }

    private void readJsonQuestions(final JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String questionId = reader.nextName();

            final Question question = questionsMap.get(questionId);
            if (question != null) {
                readJsonQuestion(reader, question);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readJsonQuestion(final JsonReader reader, final Question question) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String name = reader.nextName();
            switch (name) {
                case "text":
                    question.setText(reader.nextString());
                    break;
                case "title":
                    question.setTitle(reader.nextString());
                    break;
                case "help":
                    question.setHelp(reader.nextString());
                    break;
                case "answers": {
                    readJsonAnswers(reader, question);
                    break;
                }
                case "checkboxes": {
                    readJsonCheckboxes(reader, question);
                    break;
                }
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readJsonAnswers(final JsonReader reader, final Question question) throws IOException {
        final List<Answer> answers = question.answers;

        reader.beginObject();
        while (reader.hasNext()) {
            final String answerId = reader.nextName();

            //Get the previously created answer from the decision tree and add the translated text:
            final Answer answer = getAnswerWithId(answers, answerId);
            if (answer != null) {
                answer.setText(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readJsonCheckboxes(final JsonReader reader, final Question question) throws IOException {
        final List<Checkbox> checkboxes = question.checkboxes;

        reader.beginObject();
        while (reader.hasNext()) {
            final String answerId = reader.nextName();

            //Get the previously created checkbox from the decision tree and add the translated text:
            final Checkbox checkbox = getCheckboxWithId(checkboxes, answerId);
            if (checkbox != null) {
                checkbox.setText(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private Answer getAnswerWithId(final List<Answer> answers, final String id) {
        for (final Answer answer : answers) {
            if (TextUtils.equals(id, answer.getId())) {
                return answer;
            }
        }

        return null;
    }


    private Checkbox getCheckboxWithId(final List<Checkbox> checkboxes, final String id) {
        for (final Checkbox checkbox : checkboxes) {
            if (TextUtils.equals(id, checkbox.getId())) {
                return checkbox;
            }
        }

        return null;
    }

    private Question getFirstQuestion() {
        if (questionsMap == null) {
            return null;
        }

        return getQuestion("sloan-0"); //TODO: Awful hack. Use an ordered collection?
    }

    public Question getQuestionOrFirst(final String questionId) {
        if (TextUtils.isEmpty(questionId)) {
            return getFirstQuestion();
        } else {
            return getQuestion(questionId);
        }
    }

    public Question getQuestion(final String questionId) {
        if (questionsMap == null) {
            return null;
        }

        if (questionId == null) {
            Log.error("getQuestion(): questionId was null.");
            return null;
        }

        return questionsMap.get(questionId);
    }

    public Question getNextQuestionForAnswer(final String questionId, final String answerId) {
        final Question question = getQuestion(questionId);
        if (question == null) {
            return null;
        }

        if (question.answers == null) {
            return null;
        }

        //TODO: Use a map for performance if there are many answers:
        //For now we use a list, instead of a map, for the answers, to have an order.
        Answer answer = null;
        for (final Answer anAnswer : question.answers) {
            if (TextUtils.equals(anAnswer.getId(), answerId)) {
                answer = anAnswer;
                break;
            }
        }

        if (answer == null) {
            return null;
        }

        return getQuestion(answer.leadsToQuestionId);
    }

    public List<Question> getAllQuestions() {
        final List<Question> result = new ArrayList<>();
        for (final Question question : questionsMap.values()) {
            if (question != null) {
                result.add(question);
            }
        }

        return result;
    }

    /**
     * getElementsByTagName() is recursive, but we do not want that.
     *
     * @param parentNode
     * @param tagName
     * @return
     */
    private List<Node> getChildrenByTagName(final Element parentNode, final String tagName) {
        final List<Node> result = new ArrayList<>();

        final NodeList list = parentNode.getElementsByTagName(tagName);
        final int num = list.getLength();
        for (int i = 0; i < num; i++) {
            final Node node = list.item(i);
            if (node == null) {
                continue;
            }

            final Node itemParentNode = node.getParentNode();
            if (itemParentNode.equals(parentNode)) {
                result.add(node);
            }
        }

        return result;
    }

    /**
     * getElementsByTagName() is recursive, but we do not want that.
     *
     * @param parentNode
     * @param tagName
     * @return
     */
    private Node getFirstChildByTagName(final Element parentNode, final String tagName) {
        final NodeList list = parentNode.getElementsByTagName(tagName);
        final int num = list.getLength();
        for (int i = 0; i < num; i++) {
            final Node node = list.item(i);
            if (node == null) {
                continue;
            }

            final Node itemParentNode = node.getParentNode();
            if (itemParentNode.equals(parentNode)) {
                return node;
            }
        }

        return null;
    }

    private String getTextOfChildNode(final Element element, final String tagName) {
        final Node node = getFirstChildByTagName(element, tagName);
        if (node == null)
            return null;

        return node.getTextContent();
    }

    private Question loadQuestion(final Element questionNode) {
        final Question result = new Question(
                questionNode.getAttribute("id"),
                getTextOfChildNode(questionNode, "title"),
                getTextOfChildNode(questionNode, "text"),
                getTextOfChildNode(questionNode, "help"));

        final List<Node> listCheckboxes = getChildrenByTagName(questionNode, NODE_CHECKBOX);
        for (final Node node : listCheckboxes) {
            if (!(node instanceof Element)) {
                continue;
            }

            final Element element = (Element) node;
            final Checkbox checkbox = loadCheckbox(element);
            result.addCheckbox(checkbox);
        }

        final List<Node> listAnswers = getChildrenByTagName(questionNode, NODE_ANSWER);
        for (final Node node : listAnswers) {
            if (!(node instanceof Element)) {
                continue;
            }

            final Element element = (Element) node;
            final Answer answer = loadAnswer(element);
            result.answers.add(answer);
        }

        return result;
    }

    private Checkbox loadCheckbox(final Element checkboxNode) {
        return new Checkbox(
                checkboxNode.getAttribute("id"),
                getTextOfChildNode(checkboxNode, "text"),
                checkboxNode.getAttribute("icon"),
                Integer.parseInt(checkboxNode.getAttribute("examplesCount")));
    }

    private Answer loadAnswer(final Element answerNode) {
        return new Answer(
                answerNode.getAttribute("id"),
                getTextOfChildNode(answerNode, "text"),
                answerNode.getAttribute("icon"),
                answerNode.getAttribute("leadsTo"),
                Integer.parseInt(answerNode.getAttribute("examplesCount")));
    }

    /** This class is meant to be immutable,
     * because that's generally nice when it's possible.
     * It returns and takes String references, but String is immutable too.
     */
    static abstract class BaseButton {
        private final String id;
        private String text;
        private final String icon;
        private final int examplesCount;

        BaseButton(final String id, final String text, final String icon, int examplesCount) {
            this.id = id;
            this.text = text;
            this.icon = icon;
            this.examplesCount = examplesCount;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }


        //TODO: Remove this to make the class really immutable.
        public void setText(final String text) {
            this.text = text;
        }

        public String getIcon() {
            return icon;
        }

        int getExamplesCount() {
            return examplesCount;
        }

        public String getExampleIconName(String questionId, int exampleIndex) {
            return questionId + "_" + getId() + "_" + exampleIndex;
        }
    }

    /**
     * These are multiple-selection.
     *
     * This class is meant to be immutable,
     * because that's generally nice when it's possible.
     * It returns and takes String references, but String is immutable too.
     */
    static final class Checkbox extends BaseButton {
        Checkbox(final String id, final String text, final String icon, int examplesCount) {
            super(id, text, icon, examplesCount);
        }
    }

    /**
     * These are single selection.
     * Sometimes it's just "Done" to accept the checkbox selections.
     *
     * This class is meant to be immutable,
     * because that's generally nice when it's possible.
     * It returns and take String references, but String is immutable too.
     */
    static final class Answer extends BaseButton {
        private final String leadsToQuestionId;

        Answer(final String id, final String text, final String icon, final String leadsToQuestionId, int examplesCount) {
            super(id, text, icon, examplesCount);
            this.leadsToQuestionId = leadsToQuestionId;
        }
    }

    /**
     * A question from the Decision Tree,
     * with associated answers and checkboxes.
     *
     * This class is meant to be immutable,
     * because that's generally nice when it's possible.
     * It returns and takes references, but only to objects that are immutable too.
     */
    public static final class Question {
        private final List<Checkbox> checkboxes = new ArrayList<>();
        private final List<Answer> answers = new ArrayList<>();
        private final String id;
        private String title;
        private String text;
        private String help;

        Question(final String id, final String title, final String text, final String help) {
            this.id = id;
            this.title = title;
            this.text = text;
            this.help = help;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getText() {
            return text;
        }

        //TODO: Remove this to make the class really immutable.
        public void setText(final String text) {
            this.text = text;
        }

        public String getHelp() {
            return help;
        }

        //TODO: Remove this to make the class really immutable.
        public void setHelp(final String help) {
            this.help = help;
        }

        public boolean hasCheckboxes() {
            if (checkboxes == null) {
                return false;
            }

            if (checkboxes.size() == 0) {
                return false;
            }

            return true;
        }

        public void addCheckbox(final Checkbox checkbox) {
            checkboxes.add(checkbox);
        }

        public List<Checkbox> getCheckboxes() {
            return Collections.unmodifiableList(checkboxes);
        }

        public List<Answer> getAnswers() {
            return Collections.unmodifiableList(answers);
        }
    }

    public class DecisionTreeException extends Exception {
        DecisionTreeException(final String detail, final Exception cause) {
            super(detail, cause);
        }

        DecisionTreeException(final String detail) {
            super(detail);
        }
    }
}