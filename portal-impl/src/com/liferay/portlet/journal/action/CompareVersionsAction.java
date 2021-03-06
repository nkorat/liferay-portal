/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.journal.action;

import com.liferay.portal.kernel.portlet.PortletRequestModel;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalArticleServiceUtil;
import com.liferay.portlet.journal.util.JournalUtil;
import com.liferay.portlet.journal.util.comparator.ArticleVersionComparator;
import com.liferay.portlet.wiki.NoSuchPageException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * @author Eudaldo Alonso
 */
public class CompareVersionsAction extends PortletAction {

	@Override
	public ActionForward render(
			ActionMapping actionMapping, ActionForm actionForm,
			PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse)
		throws Exception {

		try {
			ActionUtil.getArticle(renderRequest);

			compareVersions(renderRequest, renderResponse);
		}
		catch (Exception e) {
			if (e instanceof NoSuchPageException) {
				SessionErrors.add(renderRequest, e.getClass());

				return actionMapping.findForward("portlet.journal.error");
			}
			else {
				throw e;
			}
		}

		return actionMapping.findForward("portlet.journal.compare_versions");
	}

	protected void compareVersions(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long groupId = ParamUtil.getLong(renderRequest, "groupId");
		String articleId = ParamUtil.getString(renderRequest, "articleId");

		String sourceArticleId = ParamUtil.getString(
			renderRequest, "sourceVersion");

		int index = sourceArticleId.lastIndexOf(
			EditArticleAction.VERSION_SEPARATOR);

		if (index != -1) {
			sourceArticleId =
				sourceArticleId.substring(
					index + EditArticleAction.VERSION_SEPARATOR.length(),
					sourceArticleId.length());
		}

		double sourceVersion = GetterUtil.getDouble(sourceArticleId);

		String targetArticleId = ParamUtil.getString(
			renderRequest, "targetVersion");

		index = targetArticleId.lastIndexOf(
			EditArticleAction.VERSION_SEPARATOR);

		if (index != -1) {
			targetArticleId =
				targetArticleId.substring(
					index + EditArticleAction.VERSION_SEPARATOR.length(),
					targetArticleId.length());
		}

		double targetVersion = GetterUtil.getDouble(targetArticleId);

		if ((sourceVersion == 0) && (targetVersion == 0)) {
			List<JournalArticle> articles =
				JournalArticleServiceUtil.getArticlesByArticleId(
					groupId, articleId, 0, 2,
					new ArticleVersionComparator(false));

			sourceVersion = articles.get(0).getVersion();
			targetVersion = articles.get(1).getVersion();
		}

		if (sourceVersion > targetVersion) {
			double tempVersion = targetVersion;

			targetVersion = sourceVersion;
			sourceVersion = tempVersion;
		}

		String languageId = getLanguageId(
			renderRequest, groupId, articleId, sourceVersion, targetVersion);

		String diffHtmlResults = JournalUtil.diffHtml(
			groupId, articleId, sourceVersion, targetVersion, languageId,
			new PortletRequestModel(renderRequest, renderResponse),
			themeDisplay);

		renderRequest.setAttribute(WebKeys.DIFF_HTML_RESULTS, diffHtmlResults);
		renderRequest.setAttribute(WebKeys.SOURCE_VERSION, sourceVersion);
		renderRequest.setAttribute(WebKeys.TARGET_VERSION, targetVersion);
	}

	protected String getLanguageId(
			RenderRequest renderRequest, long groupId, String articleId,
			double sourceVersion, double targetVersion)
		throws Exception {

		JournalArticle sourceArticle =
			JournalArticleLocalServiceUtil.fetchArticle(
				groupId, articleId, sourceVersion);

		JournalArticle targetArticle =
			JournalArticleLocalServiceUtil.fetchArticle(
				groupId, articleId, targetVersion);

		Set<Locale> locales = new HashSet<Locale>();

		for (String locale : sourceArticle.getAvailableLanguageIds()) {
			locales.add(LocaleUtil.fromLanguageId(locale));
		}

		for (String locale : targetArticle.getAvailableLanguageIds()) {
			locales.add(LocaleUtil.fromLanguageId(locale));
		}

		String languageId = ParamUtil.get(
			renderRequest, "languageId", targetArticle.getDefaultLanguageId());

		Locale locale = LocaleUtil.fromLanguageId(languageId);

		if (!locales.contains(locale)) {
			languageId = targetArticle.getDefaultLanguageId();
		}

		renderRequest.setAttribute(WebKeys.AVAILABLE_LOCALES, locales);
		renderRequest.setAttribute(WebKeys.LANGUAGE_ID, languageId);

		return languageId;
	}

}