package org.usergrid.security.shiro;

import static org.usergrid.utils.UUIDUtils.isUUID;

import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.springframework.util.AntPathMatcher;

public class PathBasedWildcardPermission extends WildcardPermission {

	static AntPathMatcher matcher = new AntPathMatcher();

	private static final long serialVersionUID = 1L;

	public PathBasedWildcardPermission() {
	}

	public PathBasedWildcardPermission(String wildcardString) {
		super(wildcardString);
	}

	public PathBasedWildcardPermission(String wildcardString,
			boolean caseSensitive) {
		super(wildcardString, caseSensitive);
	}

	@Override
	public List<Set<String>> getParts() {
		return super.getParts();
	}

	@Override
	public boolean implies(Permission p) {
		// By default only supports comparisons with other
		// PathBasedWildcardPermission
		if (!(p instanceof PathBasedWildcardPermission)) {
			return false;
		}

		PathBasedWildcardPermission wp = (PathBasedWildcardPermission) p;

		List<Set<String>> otherParts = wp.getParts();

		int i = 0;
		for (Set<String> otherPart : otherParts) {
			// If this permission has less parts than the other permission,
			// everything after the number of parts contained
			// in this permission is automatically implied, so return true
			if ((getParts().size() - 1) < i) {
				return true;
			} else {
				Set<String> part = getParts().get(i);
				// this part is the permission, the other part is the challenger
				if (!part.contains(WILDCARD_TOKEN)
						&& !partContainsPart(part, otherPart)) {
					return false;
				}
				i++;
			}
		}

		// If this permission has more parts than the other parts, only imply it
		// if all of the other parts are wildcards
		for (; i < getParts().size(); i++) {
			Set<String> part = getParts().get(i);
			if (!part.contains(WILDCARD_TOKEN)) {
				return false;
			}
		}

		return true;
	}

	private static boolean doCompare(String p1, String p2) {
		if (isUUID(p1)) {
			if (doCompare("/" + p1 + "/**", p2)) {
				return true;
			}
		}
		if (matcher.isPattern(p1)) {
			return matcher.match(p1, p2);
		}
		return p1.equalsIgnoreCase(p2);
	}

	public static boolean partContainsPath(Set<String> part, String path) {

		for (String subpart : part) {
			if (doCompare(subpart, path)) {
				return true;
			}
		}

		return false;
	}

	public static boolean partContainsPart(Set<String> part,
			Set<String> otherPart) {
		boolean containsAll = true;

		for (String path : otherPart) {
			boolean contains = false;
			for (String subpart : part) {
				if (doCompare(subpart, path)) {
					contains = true;
					break;
				}
			}
			containsAll &= contains;
		}

		return containsAll;
	}

}
