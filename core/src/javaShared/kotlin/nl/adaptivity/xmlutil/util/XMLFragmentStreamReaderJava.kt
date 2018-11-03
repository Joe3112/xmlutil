/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.util.impl.FragmentNamespaceContext

interface XMLFragmentStreamReaderJava : XmlReader {
    var localNamespaceContext: FragmentNamespaceContext
    val delegate: XmlReader


    override fun next(): EventType {
        val delegateNext = delegate.next()
        return when (delegateNext) {
            EventType.END_DOCUMENT  -> delegateNext
            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL       -> next()
            EventType.START_ELEMENT -> {
                if (WRAPPERNAMESPACE.contentEquals(delegate.namespaceURI)) {
                    // Special case the wrapping namespace, dropping the element.
                    next()
                } else {
                    extendNamespace()
                    delegateNext
                }
            }
            EventType.END_ELEMENT   -> if (WRAPPERNAMESPACE.contentEquals(delegate.namespaceURI)) {
                // Drop the closing tag of the wrapper as well
                delegate.next()
            } else {
                localNamespaceContext = localNamespaceContext.parent ?: localNamespaceContext
                delegateNext
            }
            else                    -> delegateNext
        }
    }

    override val namespaceStart: Int
        get() = 0

    override val namespaceEnd: Int
        get() = localNamespaceContext.size

    override fun getNamespacePrefix(index: Int): String {
        return localNamespaceContext.getPrefix(index)
    }

    override fun getNamespaceURI(index: Int): String {
        return localNamespaceContext.getNamespaceURI(index)
    }

    override val namespaceContext: NamespaceContext
        get() = localNamespaceContext

    fun extendNamespace() {
        val nsStart = delegate.namespaceStart
        val nscount = delegate.namespaceEnd - nsStart
        val prefixes = Array(nscount) { idx -> delegate.getNamespacePrefix(idx + nsStart) }
        val namespaces = Array(nscount) { idx -> delegate.getNamespaceURI(idx + nsStart) }

        localNamespaceContext = FragmentNamespaceContext(localNamespaceContext, prefixes, namespaces)
    }

    companion object {
        const val WRAPPERPPREFIX = "SDFKLJDSF"
        const val WRAPPERNAMESPACE = "http://wrapperns"

    }
}