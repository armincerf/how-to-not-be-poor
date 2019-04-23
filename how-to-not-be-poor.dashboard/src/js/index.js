import * as ReactAdmin from 'react-admin';
import dataProvider from './dataProvider.js';
import * as components from '@material-ui/core';
import * as dateInputs from 'react-admin-date-inputs';
import * as ReactRouterDOM from 'react-router-dom';
import * as redux from 'react-redux';
import * as raEnglish from 'ra-language-english';

window.ReactAdmin = ReactAdmin;
window.dataProvider = dataProvider;
window.MaterialUI = components;
window.dateInputs = dateInputs;
window.raEnglish = raEnglish;
window.ReactRouterDOM = ReactRouterDOM;
window.redux = redux;
