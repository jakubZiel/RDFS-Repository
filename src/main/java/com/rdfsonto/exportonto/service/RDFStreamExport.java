package com.rdfsonto.exportonto.service;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import com.rdfsonto.rdf4j.RDFInputOutput;


public class RDFStreamExport extends RDFInputOutput implements RDFHandler
{
    @Override
    protected IRI handlePredicate(final IRI predicate, final String tag)
    {
        return null;
    }

    @Override
    protected Resource handleSubject(final Resource subject, final String tag)
    {
        return null;
    }

    @Override
    protected Value handleObject(final Statement object, final String tag)
    {
        return null;
    }

    @Override
    public void startRDF() throws RDFHandlerException
    {

    }

    @Override
    public void endRDF() throws RDFHandlerException
    {

    }

    @Override
    public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException
    {

    }

    @Override
    public void handleStatement(final Statement st) throws RDFHandlerException
    {

    }

    @Override
    public void handleComment(final String comment) throws RDFHandlerException
    {

    }
}
